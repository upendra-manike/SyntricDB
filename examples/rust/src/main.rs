use serde::Serialize;
use std::error::Error;

const SYNTRICDB_URL: &str = "syntricdb://admin:syntricdb_secret_pass@localhost:8080/default";

#[derive(Serialize)]
struct QueryRequest<'a> {
    sql: &'a str,
    database: &'a str,
}

fn parse_connection_url(url_str: &str) -> (String, Option<(String, String)>, String) {
    let clean = url_str
        .replace("jdbc:syntricdb://", "http://")
        .replace("syntricdb://", "http://");
    if let Ok(u) = reqwest::Url::parse(&clean) {
        let host = u.host_str().unwrap_or("localhost");
        let port = u.port().unwrap_or(8080);
        let api_url = format!("http://{}:{}/api/sql", host, port);
        
        let auth = if !u.username().is_empty() {
            Some((u.username().to_string(), u.password().unwrap_or("").to_string()))
        } else {
            None
        };

        let db = u.path().trim_start_matches('/');
        let database = if db.is_empty() { "default" } else { db };

        (api_url, auth, database.to_string())
    } else {
        ("http://localhost:8080/api/sql".to_string(), None, "default".to_string())
    }
}

async fn execute_query(sql: &str) -> Result<String, Box<dyn Error>> {
    let (api_url, auth, database) = parse_connection_url(SYNTRICDB_URL);
    let client = reqwest::Client::new();
    let body = QueryRequest { sql, database: &database };
    
    let mut req = client.post(&api_url).json(&body);
    if let Some((user, pass)) = auth {
        req = req.basic_auth(user, Some(pass));
    }

    let resp = req.send().await?.text().await?;
    Ok(resp)
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    println!("=================================================");
    println!("🦀 SyntricDB Rust Integration Demo");
    println!("🔗 Connection URL: {}", SYNTRICDB_URL);
    println!("=================================================");

    let create_sql = "CREATE TABLE rust_sensors (id VARCHAR PRIMARY KEY, temp FLOAT, embedding FLOAT_VECTOR(128));";
    match execute_query(create_sql).await {
        Ok(res) => println!("✅ Create Table Response: {}", res),
        Err(e) => println!("ℹ️ Info: {}", e),
    }

    let insert_sql = "INSERT INTO rust_sensors VALUES ('sen_801', 42.5, AI_EMBED('overheating temperature sensor warning'));";
    let res2 = execute_query(insert_sql).await?;
    println!("✅ Insert Record Response: {}", res2);

    let search_sql = "SELECT id, temp FROM rust_sensors WHERE embedding SIMILAR TO 'temperature warning' TOP 1;";
    let res3 = execute_query(search_sql).await?;
    println!("\n🔍 Vector Search Results:\n{}", res3);

    println!("=================================================");
    Ok(())
}
