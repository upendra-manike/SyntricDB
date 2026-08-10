package com.syntricdb.net;

import com.syntricdb.sql.QueryExecutor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Netty ChannelHandler implementing PostgreSQL PGWire Frontend Protocol (v3.0).
 * Enables psql, pgAdmin, DBeaver, and JDBC clients to connect natively.
 */
public class PGWireServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger log = LoggerFactory.getLogger(PGWireServerHandler.class);

    private final QueryExecutor queryExecutor;
    private boolean authenticated = false;

    public PGWireServerHandler(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (!authenticated) {
            handleStartupHandshake(ctx, msg);
            return;
        }

        if (msg.readableBytes() < 5) return;
        byte msgType = msg.readByte();
        int length = msg.readInt();

        switch (msgType) {
            case 'Q': // Simple Query
                byte[] queryBytes = new byte[length - 4 - 1]; // subtract length int and null terminator
                msg.readBytes(queryBytes);
                String sql = new String(queryBytes, StandardCharsets.UTF_8).trim();
                log.info("Received PGWire SQL query: {}", sql);
                executeQuery(ctx, sql);
                break;

            case 'X': // Terminate
                ctx.close();
                break;

            default:
                log.debug("PGWire unhandled message type: '{}'", (char) msgType);
                sendReadyForQuery(ctx);
                break;
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

        // Send ParameterStatus messages
        sendParameterStatus(ctx, "server_version", "15.0 (SyntricDB-PGWire)");
        sendParameterStatus(ctx, "client_encoding", "UTF8");
        sendParameterStatus(ctx, "server_encoding", "UTF8");

        sendReadyForQuery(ctx);
        ctx.flush();
        authenticated = true;
    }

    private void executeQuery(ChannelHandlerContext ctx, String sql) {
        try {
            QueryExecutor.QueryResult result = queryExecutor.execute(sql);
            if (result.getRows() != null && !result.getRows().isEmpty()) {
                sendRowDescription(ctx, result.getColumns());
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
        sendReadyForQuery(ctx);
        ctx.flush();
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
}
