/**
 * Official Node.js Client SDK for SyntricDB AI-Native Unified Database Engine.
 * Accepts connection strings format: syntricdb://username:password@host:port/database
 */
class SyntricDBClient {
    constructor(options = 'syntricdb://admin:syntricdb_secret_pass@localhost:8080/default') {
        this.headers = { 'Content-Type': 'application/json' };
        this.database = 'default';

        let connStr = typeof options === 'string' ? options : (options.url || options.host || 'http://localhost:8080');

        if (connStr.startsWith('syntricdb://') || connStr.startsWith('jdbc:syntricdb://')) {
            const cleanUrl = connStr.replace(/^jdbc:/, '');
            try {
                const parsed = new URL(cleanUrl.replace('syntricdb://', 'http://'));
                this.host = `${parsed.protocol}//${parsed.hostname}:${parsed.port || 8080}`;
                if (parsed.pathname && parsed.pathname !== '/') {
                    this.database = parsed.pathname.replace(/^\//, '');
                }
                if (parsed.username && parsed.password) {
                    const authStr = `${decodeURIComponent(parsed.username)}:${decodeURIComponent(parsed.password)}`;
                    const b64 = Buffer.from(authStr, 'utf-8').toString('base64');
                    this.headers['Authorization'] = `Basic ${b64}`;
                }
            } catch (e) {
                this.host = 'http://localhost:8080';
            }
        } else if (connStr.startsWith('http://') || connStr.startsWith('https://')) {
            this.host = connStr.replace(/\/$/, '');
            if (typeof options === 'object' && options.apiKey) {
                this.headers['Authorization'] = `Bearer ${options.apiKey}`;
            }
        } else {
            this.host = `http://${connStr}`.replace(/\/$/, '');
        }

        this.sqlEndpoint = `${this.host}/api/sql`;
        this.vectorEndpoint = `${this.host}/api/vector/search`;
        this.ragEndpoint = `${this.host}/api/ai/rag`;
        this.clusterEndpoint = `${this.host}/api/cluster`;
    }

    _getHeaders() {
        return this.headers;
    }

    /**
     * Executes a SQL query against SyntricDB.
     * @param {string} sql 
     */
    async query(sql) {
        const response = await fetch(this.sqlEndpoint, {
            method: 'POST',
            headers: this._getHeaders(),
            body: JSON.stringify({ sql, database: this.database })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`SyntricDB Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    /**
     * Alias for query(sql)
     */
    async executeSql(sql) {
        return this.query(sql);
    }

    /**
     * Performs HNSW vector similarity search.
     * @param {string} table 
     * @param {string} column 
     * @param {string} queryText 
     * @param {number} limit 
     */
    async vectorSearch(table, column = 'embedding', queryText = '', limit = 5) {
        const response = await fetch(this.vectorEndpoint, {
            method: 'POST',
            headers: this._getHeaders(),
            body: JSON.stringify({ database: this.database, table, column, query: queryText, limit })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`SyntricDB Vector Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    /**
     * Executes Retrieval-Augmented Generation (RAG) context search.
     * @param {string} prompt 
     * @param {string} table 
     * @param {string} column 
     * @param {number} limit 
     */
    async askRag(prompt, table = 'users', column = 'embedding', limit = 3) {
        const response = await fetch(this.ragEndpoint, {
            method: 'POST',
            headers: this._getHeaders(),
            body: JSON.stringify({ database: this.database, prompt, table, column, limit })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`SyntricDB RAG Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    /**
     * Retrieves cluster topology, status, and node health.
     */
    async getClusterStatus() {
        const response = await fetch(this.clusterEndpoint, {
            method: 'GET',
            headers: this._getHeaders()
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`SyntricDB Cluster Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    /**
     * Tests connectivity to the SyntricDB server.
     */
    async testConnection() {
        try {
            const status = await this.getClusterStatus();
            return { connected: true, info: status };
        } catch (err) {
            return { connected: false, error: err.message };
        }
    }
}

module.exports = { SyntricDBClient };
