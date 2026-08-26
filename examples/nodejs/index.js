const SYNTRICDB_URL = 'syntricdb://admin:syntricdb_secret_pass@localhost:8080/default';

function parseConnectionUrl(connStr) {
    const cleanUrl = connStr.replace(/^jdbc:/, '');
    const parsed = new URL(cleanUrl.replace('syntricdb://', 'http://'));
    const host = `${parsed.protocol}//${parsed.hostname}:${parsed.port || 8080}/api/sql`;
    const headers = { 'Content-Type': 'application/json' };
    if (parsed.username && parsed.password) {
        const authStr = `${decodeURIComponent(parsed.username)}:${decodeURIComponent(parsed.password)}`;
        headers['Authorization'] = `Basic ${Buffer.from(authStr, 'utf-8').toString('base64')}`;
    }
    const database = parsed.pathname ? parsed.pathname.replace(/^\//, '') : 'default';
    return { apiUrl: host, headers, database };
}

async function executeQuery(sql) {
    const { apiUrl, headers, database } = parseConnectionUrl(SYNTRICDB_URL);
    const response = await fetch(apiUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify({ sql, database })
    });
    
    if (!response.ok) {
        throw new Error(`HTTP Error ${response.status}: ${await response.text()}`);
    }
    return await response.json();
}

async function main() {
    console.log('=================================================');
    console.log('💚 SyntricDB Node.js Integration Demo');
    console.log(`🔗 Connection URL: ${SYNTRICDB_URL}`);
    console.log('=================================================');

    try {
        await executeQuery(`
            CREATE TABLE node_services (
                id VARCHAR PRIMARY KEY,
                name VARCHAR,
                region VARCHAR,
                latency_ms FLOAT,
                embedding FLOAT_VECTOR(128)
            );
        `);
        console.log('✅ Created "node_services" table.');
    } catch (e) {
        console.log('ℹ️ Table info:', e.message);
    }

    const insertRes = await executeQuery(`
        INSERT INTO node_services VALUES (
            'srv_501',
            'Authentication Microservice',
            'us-east-1',
            1.2,
            AI_EMBED('Authentication JWT OAuth2 security microservice')
        );
    `);
    console.log('✅ Inserted Service Record:', insertRes.message || 'OK');

    const searchRes = await executeQuery(`
        SELECT id, name, region, latency_ms 
        FROM node_services 
        WHERE region = 'us-east-1' 
          AND embedding SIMILAR TO 'security authentication microservice' 
        TOP 1;
    `);
    console.log('\n🔍 Vector Search Results:');
    console.log(JSON.stringify(searchRes, null, 2));

    console.log('=================================================');
}

main().catch(console.error);
