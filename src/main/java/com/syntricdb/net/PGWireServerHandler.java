package com.syntricdb.net;

import com.syntricdb.sql.QueryExecutor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Netty ChannelHandler implementing PostgreSQL PGWire Frontend Protocol (v3.0).
 * Supports both Simple Query ('Q') and Extended Query Protocol ('P', 'B', 'D', 'E', 'S')
 * for native compatibility with PostgreSQL JDBC drivers, Spring Data JPA, Hibernate, DBeaver, and psql.
 */
public class PGWireServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger log = LoggerFactory.getLogger(PGWireServerHandler.class);

    private final QueryExecutor queryExecutor;
    private boolean authenticated = false;

    private final Map<String, String> preparedStatements = new HashMap<>();
    private final Map<String, String> portals = new HashMap<>();
    private String lastDescribedQuery = null;

    public PGWireServerHandler(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (!authenticated) {
            handleStartupHandshake(ctx, msg);
            return;
        }

        while (msg.readableBytes() >= 5) {
            msg.markReaderIndex();
            byte msgType = msg.readByte();
            int length = msg.readInt();

            if (msg.readableBytes() < length - 4) {
                msg.resetReaderIndex();
                break;
            }

            ByteBuf body = msg.readSlice(length - 4);
            switch (msgType) {
                case 'Q': // Simple Query
                    String sql = readString(body).trim();
                    log.info("Received PGWire SQL query: {}", sql);
                    executeQuery(ctx, sql, true);
                    break;

                case 'P': // Parse Prepared Statement
                    String stmtName = readString(body);
                    String prepareSql = readString(body);
                    log.debug("PGWire Parse statement '{}': {}", stmtName, prepareSql);
                    preparedStatements.put(stmtName, prepareSql);
                    sendParseComplete(ctx);
                    break;

                case 'B': // Bind Portal
                    String portalName = readString(body);
                    String sourceStmtName = readString(body);
                    String boundSql = bindParameters(sourceStmtName, body);
                    log.debug("PGWire Bind portal '{}' -> {}", portalName, boundSql);
                    portals.put(portalName, boundSql);
                    sendBindComplete(ctx);
                    break;

                case 'D': // Describe
                    byte descType = body.readableBytes() > 0 ? body.readByte() : 0;
                    String descName = body.readableBytes() > 0 ? readString(body) : "";
                    log.debug("PGWire Describe type '{}' name '{}'", (char) descType, descName);
                    String q = portals.getOrDefault(descName, preparedStatements.getOrDefault(descName, ""));
                    if (!q.isEmpty()) {
                        try {
                            QueryExecutor.QueryResult res = queryExecutor.execute(q);
                            if (res.getRows() != null && !res.getRows().isEmpty()) {
                                sendRowDescription(ctx, res.getColumns());
                                lastDescribedQuery = q;
                            } else {
                                sendNoData(ctx);
                            }
                        } catch (Exception e) {
                            sendNoData(ctx);
                        }
                    } else {
                        sendNoData(ctx);
                    }
                    break;

                case 'E': // Execute
                    String execPortal = readString(body);
                    int maxRows = body.readableBytes() >= 4 ? body.readInt() : 0;
                    String queryToRun = portals.getOrDefault(execPortal, preparedStatements.getOrDefault(execPortal, ""));
                    log.info("PGWire Execute portal '{}': {}", execPortal, queryToRun);
                    if (!queryToRun.isEmpty()) {
                        executeQuery(ctx, queryToRun, false);
                    } else {
                        sendCommandComplete(ctx, "OK");
                    }
                    break;

                case 'S': // Sync
                    sendReadyForQuery(ctx);
                    ctx.flush();
                    break;

                case 'X': // Terminate
                    ctx.close();
                    return;

                default:
                    log.debug("PGWire unhandled message type: '{}'", (char) msgType);
                    sendReadyForQuery(ctx);
                    ctx.flush();
                    break;
            }
        }
    }

    private void handleStartupHandshake(ChannelHandlerContext ctx, ByteBuf msg) {
        int length = msg.readInt();
        int protocol = msg.readInt();

        if (protocol == 80877103) { // SSLRequest code
            log.info("PGWire SSLRequest received - sending 'N' (No SSL)");
            ByteBuf response = ctx.alloc().buffer(1);
            response.writeByte('N');
            ctx.writeAndFlush(response);
            return;
        }

        log.info("PGWire StartupMessage v3.0 received (Length: {})", length);

        // Send AuthenticationOk ('R', len 8, code 0)
        ByteBuf authOk = ctx.alloc().buffer(9);
        authOk.writeByte('R');
        authOk.writeInt(8);
        authOk.writeInt(0);
        ctx.write(authOk);

        // Send BackendKeyData ('K', len 12)
        ByteBuf keyData = ctx.alloc().buffer(13);
        keyData.writeByte('K');
        keyData.writeInt(12);
        keyData.writeInt(1001); // Process ID
        keyData.writeInt(54321); // Secret Key
        ctx.write(keyData);

        // Send ParameterStatus messages
        sendParameterStatus(ctx, "server_version", "15.0 (SyntricDB-PGWire)");
        sendParameterStatus(ctx, "client_encoding", "UTF8");
        sendParameterStatus(ctx, "server_encoding", "UTF8");
        sendParameterStatus(ctx, "integer_datetimes", "on");
        sendParameterStatus(ctx, "StandardConformingStrings", "on");
        sendParameterStatus(ctx, "session_authorization", "admin");

        sendReadyForQuery(ctx);
        ctx.flush();
        authenticated = true;
    }

    private String bindParameters(String stmtName, ByteBuf body) {
        String baseSql = preparedStatements.getOrDefault(stmtName, "");
        if (baseSql.isEmpty()) {
            return "";
        }

        try {
            // Read parameter format codes
            if (body.readableBytes() < 2) return baseSql;
            short numFormatCodes = body.readShort();
            for (int i = 0; i < numFormatCodes; i++) {
                if (body.readableBytes() >= 2) body.readShort();
            }

            // Read parameter values
            if (body.readableBytes() < 2) return baseSql;
            short numParams = body.readShort();
            String resultSql = baseSql;

            for (int p = 1; p <= numParams; p++) {
                if (body.readableBytes() < 4) break;
                int valLen = body.readInt();
                String valStr = "";
                if (valLen > 0 && body.readableBytes() >= valLen) {
                    byte[] bytes = new byte[valLen];
                    body.readBytes(bytes);
                    valStr = new String(bytes, StandardCharsets.UTF_8);
                }

                String paramMarker = "$" + p;
                if (resultSql.contains(paramMarker)) {
                    String replacement;
                    if (valLen < 0) {
                        replacement = "NULL";
                    } else if (valStr.matches("^-?\\d+(\\.\\d+)?$")) {
                        replacement = valStr;
                    } else {
                        replacement = "'" + valStr.replace("'", "''") + "'";
                    }
                    resultSql = resultSql.replace(paramMarker, replacement);
                }
            }
            return resultSql;
        } catch (Exception e) {
            log.warn("Error parsing PGWire bind parameters: {}", e.getMessage());
            return baseSql;
        }
    }

    private void executeQuery(ChannelHandlerContext ctx, String sql, boolean sendReady) {
        try {
            QueryExecutor.QueryResult result = queryExecutor.execute(sql);
            if (result.getRows() != null && !result.getRows().isEmpty()) {
                if (lastDescribedQuery == null || !lastDescribedQuery.equals(sql)) {
                    sendRowDescription(ctx, result.getColumns());
                }
                lastDescribedQuery = null;
                for (Map<String, Object> row : result.getRows()) {
                    sendDataRow(ctx, row, result.getColumns());
                }
                sendCommandComplete(ctx, "SELECT " + result.getRows().size());
            } else {
                sendCommandComplete(ctx, "OK");
            }
        } catch (Exception e) {
            sendErrorResponse(ctx, e.getMessage());
        }
        if (sendReady) {
            sendReadyForQuery(ctx);
        }
        ctx.flush();
    }

    private void sendParseComplete(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(5);
        buf.writeByte('1');
        buf.writeInt(4);
        ctx.write(buf);
    }

    private void sendBindComplete(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(5);
        buf.writeByte('2');
        buf.writeInt(4);
        ctx.write(buf);
    }

    private void sendNoData(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(5);
        buf.writeByte('n');
        buf.writeInt(4);
        ctx.write(buf);
    }

    private void sendParameterStatus(ChannelHandlerContext ctx, String name, String value) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] valBytes = value.getBytes(StandardCharsets.UTF_8);
        int len = 4 + nameBytes.length + 1 + valBytes.length + 1;

        ByteBuf buf = ctx.alloc().buffer(1 + len);
        buf.writeByte('S');
        buf.writeInt(len);
        buf.writeBytes(nameBytes);
        buf.writeByte(0);
        buf.writeBytes(valBytes);
        buf.writeByte(0);
        ctx.write(buf);
    }

    private void sendRowDescription(ChannelHandlerContext ctx, List<String> columns) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte('T');
        int markIndex = buf.writerIndex();
        buf.writeInt(0); // placeholder for length
        buf.writeShort(columns.size());

        for (String col : columns) {
            buf.writeBytes(col.getBytes(StandardCharsets.UTF_8));
            buf.writeByte(0);
            buf.writeInt(0); // table OID
            buf.writeShort(0); // column attribute number
            buf.writeInt(25); // type OID (text = 25)
            buf.writeShort(-1); // type size
            buf.writeInt(-1); // type modifier
            buf.writeShort(0); // format code (text = 0)
        }

        buf.setInt(markIndex, buf.writerIndex() - markIndex);
        ctx.write(buf);
    }

    private void sendDataRow(ChannelHandlerContext ctx, Map<String, Object> row, List<String> columns) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte('D');
        int markIndex = buf.writerIndex();
        buf.writeInt(0);
        buf.writeShort(columns.size());

        for (String col : columns) {
            Object val = row.get(col);
            if (val == null) {
                buf.writeInt(-1); // NULL
            } else {
                byte[] bytes = val.toString().getBytes(StandardCharsets.UTF_8);
                buf.writeInt(bytes.length);
                buf.writeBytes(bytes);
            }
        }

        buf.setInt(markIndex, buf.writerIndex() - markIndex);
        ctx.write(buf);
    }

    private void sendCommandComplete(ChannelHandlerContext ctx, String tag) {
        byte[] bytes = tag.getBytes(StandardCharsets.UTF_8);
        int len = 4 + bytes.length + 1;

        ByteBuf buf = ctx.alloc().buffer(1 + len);
        buf.writeByte('C');
        buf.writeInt(len);
        buf.writeBytes(bytes);
        buf.writeByte(0);
        ctx.write(buf);
    }

    private void sendReadyForQuery(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(6);
        buf.writeByte('Z');
        buf.writeInt(5);
        buf.writeByte('I'); // Idle transaction state
        ctx.write(buf);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, String message) {
        byte[] msgBytes = (message != null ? message : "Error executing query").getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte('E');
        int markIndex = buf.writerIndex();
        buf.writeInt(0);

        buf.writeByte('S'); buf.writeBytes("ERROR".getBytes(StandardCharsets.UTF_8)); buf.writeByte(0);
        buf.writeByte('M'); buf.writeBytes(msgBytes); buf.writeByte(0);
        buf.writeByte(0);

        buf.setInt(markIndex, buf.writerIndex() - markIndex);
        ctx.write(buf);
    }

    private String readString(ByteBuf buf) {
        int length = 0;
        int startIndex = buf.readerIndex();
        while (buf.isReadable()) {
            if (buf.readByte() == 0) {
                break;
            }
            length++;
        }
        byte[] bytes = new byte[length];
        buf.getBytes(startIndex, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
