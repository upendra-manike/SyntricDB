package com.syntricdb.sql;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.engine.schema.ColumnDef;
import com.syntricdb.engine.schema.ColumnType;
import com.syntricdb.engine.schema.Tuple;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLParser {
    private final AIEngine aiEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SQLParser(AIEngine aiEngine) {
        this.aiEngine = aiEngine;
    }

    public AST.Statement parse(String sql) throws Exception {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query cannot be empty.");
        }

        String trimmed = sql.trim().replaceAll(";$", "");
        String uppercase = trimmed.toUpperCase();

        if (uppercase.startsWith("CREATE DATABASE")) {
            return parseCreateDatabase(trimmed);
        } else if (uppercase.startsWith("DROP DATABASE")) {
            return parseDropDatabase(trimmed);
        } else if (uppercase.startsWith("USE ")) {
            return parseUseDatabase(trimmed);
        } else if (uppercase.equals("SHOW DATABASES") || uppercase.equals("SHOW SCHEMAS")) {
            return new AST.ShowDatabasesStatement();
        } else if (uppercase.startsWith("SHOW TABLES")) {
            return parseShowTables(trimmed);
        } else if (uppercase.startsWith("CREATE TABLE")) {
            return parseCreateTable(trimmed);
        } else if (uppercase.startsWith("INSERT INTO")) {
            return parseInsert(trimmed);
        } else if (uppercase.startsWith("SELECT AI_RAG") || uppercase.startsWith("SELECT  AI_RAG")) {
            return parseSelectAiRag(trimmed);
        } else if (uppercase.startsWith("SELECT")) {
            return parseSelect(trimmed);
        } else if (uppercase.startsWith("UPDATE")) {
            return parseUpdate(trimmed);
        } else if (uppercase.startsWith("DELETE")) {
            return parseDelete(trimmed);
        } else if (uppercase.startsWith("PUBLISH INTO")) {
            return parseStreamPublish(trimmed);
        } else if (uppercase.startsWith("SET ")) {
            return new AST.SetStatement(trimmed);
        } else if (uppercase.startsWith("BEGIN") || uppercase.startsWith("COMMIT") || uppercase.startsWith("ROLLBACK") || uppercase.startsWith("START TRANSACTION") || uppercase.startsWith("SHOW ")) {
            return new AST.NoOpStatement();
        }


        throw new IllegalArgumentException("Unsupported SQL statement syntax: " + trimmed);
    }

    private AST.SelectStatement parseSelectAiRag(String sql) {
        Pattern ragP = Pattern.compile("SELECT\\s+AI_RAG\\s*\\((?:['\"](.*?)['\"]|(\\?))\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher ragM = ragP.matcher(sql);
        String prompt = "AI Query Prompt";
        if (ragM.find()) {
            if (ragM.group(1) != null) prompt = ragM.group(1);
        }
        AST.SelectStatement stmt = new AST.SelectStatement(null);
        stmt.getSelectItems().add(new AST.SelectItem(prompt, "ai_rag", "AI_RAG", new String[]{prompt}));
        return stmt;
    }

    private AST.CreateDatabaseStatement parseCreateDatabase(String sql) {
        Pattern p = Pattern.compile("CREATE\\s+DATABASE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid CREATE DATABASE syntax. Expected: CREATE DATABASE <database_name>");
        }
        return new AST.CreateDatabaseStatement(m.group(1).trim());
    }

    private AST.DropDatabaseStatement parseDropDatabase(String sql) {
        Pattern p = Pattern.compile("DROP\\s+DATABASE\\s+(?:IF\\s+EXISTS\\s+)?([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid DROP DATABASE syntax. Expected: DROP DATABASE <database_name>");
        }
        return new AST.DropDatabaseStatement(m.group(1).trim());
    }

    private AST.UseDatabaseStatement parseUseDatabase(String sql) {
        Pattern p = Pattern.compile("USE\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid USE syntax. Expected: USE <database_name>");
        }
        return new AST.UseDatabaseStatement(m.group(1).trim());
    }

    private AST.ShowTablesStatement parseShowTables(String sql) {
        Pattern p = Pattern.compile("SHOW\\s+TABLES(?:\\s+(?:FROM|IN)\\s+([a-zA-Z0-9_]+))?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        if (m.find() && m.group(1) != null) {
            return new AST.ShowTablesStatement(m.group(1).trim());
        }
        return new AST.ShowTablesStatement(null);
    }

    private AST.CreateTableStatement parseCreateTable(String sql) {
        // Syntax: CREATE TABLE [IF NOT EXISTS] [db.]users (id VARCHAR PRIMARY KEY, age INT, bio VARCHAR, embedding FLOAT_VECTOR(128))
        Pattern p = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-zA-Z0-9_\\.]+)\\s*\\((.*)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid CREATE TABLE syntax.");
        }

        String tableName = m.group(1).trim();
        String colsBody = m.group(2).trim();

        AST.CreateTableStatement stmt = new AST.CreateTableStatement(tableName);
        String[] colDefs = colsBody.split(",");

        for (String colDefStr : colDefs) {
            colDefStr = colDefStr.trim();
            String[] parts = colDefStr.split("\\s+");
            if (parts.length < 2) continue;

            String colName = parts[0];
            String typeStr = parts[1].toUpperCase();
            boolean isPk = colDefStr.toUpperCase().contains("PRIMARY KEY");
            boolean isIndexed = colDefStr.toUpperCase().contains("INDEX") || isPk;

            ColumnType type = ColumnType.VARCHAR;
            int vectorDim = 128;

            if (typeStr.startsWith("INT")) {
                type = ColumnType.INT;
            } else if (typeStr.startsWith("BIGINT")) {
                type = ColumnType.BIGINT;
            } else if (typeStr.startsWith("DOUBLE") || typeStr.startsWith("FLOAT")) {
                type = ColumnType.DOUBLE;
            } else if (typeStr.startsWith("BOOLEAN")) {
                type = ColumnType.BOOLEAN;
            } else if (typeStr.startsWith("FLOAT_VECTOR") || typeStr.startsWith("VECTOR")) {
                type = ColumnType.FLOAT_VECTOR;
                Pattern dimPattern = Pattern.compile("\\((\\d+)\\)");
                Matcher dimM = dimPattern.matcher(typeStr);
                if (dimM.find()) {
                    vectorDim = Integer.parseInt(dimM.group(1));
                }
            }

            stmt.addColumn(new ColumnDef(colName, type, vectorDim, isPk, isIndexed));
        }

        return stmt;
    }

    private AST.InsertStatement parseInsert(String sql) throws Exception {
        Pattern p = Pattern.compile("INSERT\\s+INTO\\s+([a-zA-Z0-9_\\.]+)(?:\\s*\\((.*?)\\))?\\s+VALUES\\s*(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid INSERT INTO syntax.");
        }

        String rawTable = m.group(1).trim();
        String tableName = rawTable.split("\\s+")[0].trim();
        String columnsStr = m.group(2) != null ? m.group(2).trim() : null;
        String valuesBody = m.group(3).trim();

        Tuple tuple = new Tuple();

        if (valuesBody.startsWith("{")) {
            Map<String, Object> map = objectMapper.readValue(valuesBody, Map.class);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String && entry.getValue().toString().startsWith("AI_EMBED(")) {
                    String text = extractAiEmbedArg(entry.getValue().toString());
                    tuple.set(entry.getKey(), aiEngine.aiEmbed(text));
                } else {
                    tuple.set(entry.getKey(), entry.getValue());
                }
            }
        } else {
            if (valuesBody.startsWith("(") && valuesBody.endsWith(")")) {
                valuesBody = valuesBody.substring(1, valuesBody.length() - 1);
            }
            List<String> colNames = new ArrayList<>();
            if (columnsStr != null && !columnsStr.isBlank()) {
                for (String c : columnsStr.split(",")) {
                    String col = c.trim();
                    if (col.contains(".")) {
                        col = col.substring(col.lastIndexOf('.') + 1);
                    }
                    colNames.add(col);
                }
            }

            List<String> tokens = parseCSVValues(valuesBody);
            for (int i = 0; i < tokens.size(); i++) {
                String valStr = tokens.get(i).trim();
                String colName = (i < colNames.size()) ? colNames.get(i) : "val_" + i;
                if (valStr.toUpperCase().startsWith("AI_EMBED(")) {
                    String text = extractAiEmbedArg(valStr);
                    tuple.set(colName, aiEngine.aiEmbed(text));
                } else {
                    tuple.set(colName, unquote(valStr));
                }
            }
        }

        return new AST.InsertStatement(tableName, tuple);
    }

    private AST.SelectStatement parseSelect(String sql) {
        Pattern p = Pattern.compile("SELECT\\s+(.*?)\\s+FROM\\s+([a-zA-Z0-9_\\.\\s]+?)(?:\\s+WHERE\\s+(.*?))?(?:\\s+ORDER\\s+BY\\s+([a-zA-Z0-9_\\.]+)(?:\\s+(ASC|DESC))?)?(?:\\s+LIMIT\\s+(\\d+))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid SELECT query syntax.");
        }

        String selectBody = m.group(1).trim();
        String rawTable = m.group(2).trim();
        String tableName = rawTable.split("\\s+")[0].trim();
        String whereBody = m.group(3) != null ? m.group(3).trim() : null;
        String orderByCol = m.group(4) != null ? m.group(4).trim() : null;
        if (orderByCol != null && orderByCol.contains(".")) {
            orderByCol = orderByCol.substring(orderByCol.lastIndexOf('.') + 1);
        }
        String orderDir = m.group(5) != null ? m.group(5).trim() : "ASC";
        String limitStr = m.group(6) != null ? m.group(6).trim() : null;

        AST.SelectStatement stmt = new AST.SelectStatement(tableName);

        // Select items
        String[] items = selectBody.split(",");
        for (String item : items) {
            item = item.trim();
            if (item.contains(".")) {
                item = item.substring(item.lastIndexOf('.') + 1);
            }
            if (item.toUpperCase().startsWith("AI_SUMMARIZE(")) {
                String arg = extractFunctionArg(item);
                stmt.getSelectItems().add(new AST.SelectItem(arg, "ai_summary", "AI_SUMMARIZE", new String[]{arg}));
            } else if (item.toUpperCase().startsWith("AI_CLASSIFY(")) {
                String[] args = extractFunctionArgs(item);
                stmt.getSelectItems().add(new AST.SelectItem(args[0], "ai_class", "AI_CLASSIFY", args));
            } else {
                stmt.getSelectItems().add(new AST.SelectItem(item));
            }
        }

        // Where clauses
        if (whereBody != null && !whereBody.isBlank()) {
            String[] conds = whereBody.split("(?i)\\s+AND\\s+");
            for (String cond : conds) {
                cond = cond.trim();
                // Vector similarity check: e.g., embedding SIMILAR TO 'Java Engineer'
                if (cond.toUpperCase().contains("SIMILAR TO")) {
                    Pattern simP = Pattern.compile("([a-zA-Z0-9_\\.]+)\\s+SIMILAR\\s+TO\\s+['\"](.*?)['\"](?:\\s+TOP\\s+(\\d+))?", Pattern.CASE_INSENSITIVE);
                    Matcher simM = simP.matcher(cond);
                    if (simM.find()) {
                        String rawVecCol = simM.group(1);
                        String vecCol = rawVecCol.contains(".") ? rawVecCol.substring(rawVecCol.lastIndexOf('.') + 1) : rawVecCol;
                        String queryText = simM.group(2);
                        int k = simM.group(3) != null ? Integer.parseInt(simM.group(3)) : 10;
                        stmt.setVectorSearchCondition(new AST.VectorSearchCondition(vecCol, queryText, aiEngine.aiEmbed(queryText), k, 1.0));
                    }
                }
                // Full text match: e.g., MATCH(bio, 'engineer')
                else if (cond.toUpperCase().startsWith("MATCH(")) {
                    String[] args = extractFunctionArgs(cond);
                    String rawCol = args[0];
                    String col = rawCol.contains(".") ? rawCol.substring(rawCol.lastIndexOf('.') + 1) : rawCol;
                    stmt.setFullTextCondition(new AST.FullTextCondition(col, unquote(args[1])));
                }
                // Scalar conditions: e.g., city='Hyderabad' or age>30 or p1_0.id='test_prod_1'
                else {
                    Pattern scalarP = Pattern.compile("([a-zA-Z0-9_\\.]+)\\s*(=|!=|>|<|>=|<=)\\s*(.*)");
                    Matcher scalarM = scalarP.matcher(cond);
                    if (scalarM.find()) {
                        String rawCol = scalarM.group(1);
                        String col = rawCol.contains(".") ? rawCol.substring(rawCol.lastIndexOf('.') + 1) : rawCol;
                        String op = scalarM.group(2);
                        String val = unquote(scalarM.group(3));
                        stmt.getWhereConditions().add(new AST.Condition(col, op, parseLiteral(val)));
                    }
                }
            }
        }

        if (orderByCol != null) {
            stmt.setOrderByColumn(orderByCol);
            stmt.setOrderByDesc("DESC".equalsIgnoreCase(orderDir));
        }

        if (limitStr != null) {
            stmt.setLimit(Integer.parseInt(limitStr));
        }

        return stmt;
    }

    private AST.StreamPublishStatement parseStreamPublish(String sql) throws Exception {
        Pattern p = Pattern.compile("PUBLISH\\s+INTO\\s+([a-zA-Z0-9_]+)\\s+VALUES\\s*(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid PUBLISH INTO syntax.");
        }
        String topic = m.group(1).trim();
        String json = m.group(2).trim();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        return new AST.StreamPublishStatement(topic, map);
    }

    private String extractAiEmbedArg(String expr) {
        Pattern p = Pattern.compile("AI_EMBED\\(['\"]?(.*?)['\"]?\\)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(expr);
        return m.find() ? m.group(1) : expr;
    }

    private String extractFunctionArg(String expr) {
        int start = expr.indexOf("(");
        int end = expr.lastIndexOf(")");
        return (start >= 0 && end > start) ? expr.substring(start + 1, end).trim() : expr;
    }

    private String[] extractFunctionArgs(String expr) {
        String inner = extractFunctionArg(expr);
        String[] parts = inner.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = unquote(parts[i].trim());
        }
        return parts;
    }

    private List<String> parseCSVValues(String csv) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : csv.toCharArray()) {
            if (c == '\'' || c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    private String unquote(String val) {
        if (val == null) return null;
        val = val.trim();
        if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private Object parseLiteral(String val) {
        if (val == null) return null;
        try { return Integer.parseInt(val); } catch (Exception ignored) {}
        try { return Double.parseDouble(val); } catch (Exception ignored) {}
        if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
            return Boolean.parseBoolean(val);
        }
        return val;
    }

    private AST.UpdateStatement parseUpdate(String sql) {
        Pattern p = Pattern.compile("UPDATE\\s+([a-zA-Z0-9_\\.]+)\\s+SET\\s+(.*?)(?:\\s+WHERE\\s+(.*))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid UPDATE syntax.");
        }
        String tableName = m.group(1).trim();
        String setBody = m.group(2).trim();
        String whereBody = m.group(3) != null ? m.group(3).trim() : null;

        AST.UpdateStatement stmt = new AST.UpdateStatement(tableName);
        String[] assignments = setBody.split(",");
        for (String assign : assignments) {
            String[] kv = assign.split("=");
            if (kv.length == 2) {
                stmt.addAssignment(kv[0].trim(), parseLiteral(unquote(kv[1].trim())));
            }
        }
        if (whereBody != null && !whereBody.isBlank()) {
            String[] conds = whereBody.split("(?i)\\s+AND\\s+");
            for (String cond : conds) {
                Pattern scalarP = Pattern.compile("([a-zA-Z0-9_]+)\\s*(=|!=|>|<|>=|<=)\\s*(.*)");
                Matcher scalarM = scalarP.matcher(cond.trim());
                if (scalarM.find()) {
                    stmt.getWhereConditions().add(new AST.Condition(scalarM.group(1), scalarM.group(2), parseLiteral(unquote(scalarM.group(3)))));
                }
            }
        }
        return stmt;
    }

    private AST.DeleteStatement parseDelete(String sql) {
        Pattern p = Pattern.compile("DELETE\\s+FROM\\s+([a-zA-Z0-9_\\.]+)(?:\\s+WHERE\\s+(.*))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid DELETE syntax.");
        }
        String tableName = m.group(1).trim();
        String whereBody = m.group(2) != null ? m.group(2).trim() : null;

        AST.DeleteStatement stmt = new AST.DeleteStatement(tableName);
        if (whereBody != null && !whereBody.isBlank()) {
            String[] conds = whereBody.split("(?i)\\s+AND\\s+");
            for (String cond : conds) {
                Pattern scalarP = Pattern.compile("([a-zA-Z0-9_]+)\\s*(=|!=|>|<|>=|<=)\\s*(.*)");
                Matcher scalarM = scalarP.matcher(cond.trim());
                if (scalarM.find()) {
                    stmt.getWhereConditions().add(new AST.Condition(scalarM.group(1), scalarM.group(2), parseLiteral(unquote(scalarM.group(3)))));
                }
            }
        }
        return stmt;
    }
}
