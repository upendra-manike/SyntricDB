import urllib.parse
import base64
import requests
import json
from typing import Dict, Any, List, Optional

class SyntricDBClient:
    """
    Official Python Client SDK for SyntricDB AI-Native Unified Database Engine.
    Accepts connection strings format: syntricdb://username:password@host:port/database
    or HTTP endpoints.
    """
    def __init__(self, connection_string: str = "syntricdb://admin:syntricdb_secret_pass@localhost:8080/default", api_key: Optional[str] = None):
        self.headers = {"Content-Type": "application/json"}
        self.database = "default"

        if connection_string.startswith("syntricdb://") or connection_string.startswith("jdbc:syntricdb://"):
            url_str = connection_string.replace("jdbc:syntricdb://", "http://").replace("syntricdb://", "http://")
            parsed = urllib.parse.urlparse(url_str)
            host_name = parsed.hostname or "localhost"
            port_num = parsed.port or 8080
            self.host = f"http://{host_name}:{port_num}"
            if parsed.path and parsed.path.strip("/"):
                self.database = parsed.path.strip("/")
            
            if parsed.username and parsed.password:
                user_pass = f"{parsed.username}:{parsed.password}"
                b64_auth = base64.b64encode(user_pass.encode("utf-8")).decode("utf-8")
                self.headers["Authorization"] = f"Basic {b64_auth}"
        elif connection_string.startswith("http://") or connection_string.startswith("https://"):
            self.host = connection_string.rstrip("/")
            if api_key:
                self.headers["Authorization"] = f"Bearer {api_key}"
        else:
            self.host = f"http://{connection_string}".rstrip("/")

        self.sql_endpoint = f"{self.host}/api/sql"
        self.vector_endpoint = f"{self.host}/api/vector/search"
        self.rag_endpoint = f"{self.host}/api/ai/rag"
        self.cluster_endpoint = f"{self.host}/api/cluster"

    def query(self, sql: str) -> Dict[str, Any]:
        """
        Executes a SQL query against SyntricDB.
        """
        payload = {"sql": sql, "database": self.database}
        response = requests.post(self.sql_endpoint, json=payload, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB Error ({response.status_code}): {response.text}")

    def execute_sql(self, sql: str) -> Dict[str, Any]:
        """Alias for query(sql)"""
        return self.query(sql)

    def vector_search(self, table: str, column: str = "embedding", query: str = "", limit: int = 5) -> Dict[str, Any]:
        """
        Performs sub-millisecond HNSW vector similarity search.
        """
        payload = {
            "database": self.database,
            "table": table,
            "column": column,
            "query": query,
            "limit": limit
        }
        response = requests.post(self.vector_endpoint, json=payload, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB Vector Error ({response.status_code}): {response.text}")

    def ask_rag(self, prompt: str, table: str = "users", column: str = "embedding", limit: int = 3) -> Dict[str, Any]:
        """
        Executes Retrieval-Augmented Generation (RAG) context search.
        """
        payload = {
            "database": self.database,
            "prompt": prompt,
            "table": table,
            "column": column,
            "limit": limit
        }
        response = requests.post(self.rag_endpoint, json=payload, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB RAG Error ({response.status_code}): {response.text}")

    def get_cluster_status(self) -> Dict[str, Any]:
        """
        Retrieves cluster topology, Raft consensus status, and node health.
        """
        response = requests.get(self.cluster_endpoint, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB Cluster Error ({response.status_code}): {response.text}")

    def test_connection(self) -> tuple[bool, Dict[str, Any]]:
        """
        Tests connectivity to the SyntricDB server.
        """
        try:
            status = self.get_cluster_status()
            return True, status
        except Exception as e:
            return False, {"error": str(e)}
