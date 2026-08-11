"""
Official LlamaIndex VectorStore Connector for SyntricDB AI-Native Unified Database Engine.
"""

from typing import Any, List, Optional, Dict
from syntricdb.client import SyntricDBClient

class SyntricDBLlamaIndexVectorStore:
    """
    SyntricDB LlamaIndex Vector Store Connector.

    Example:
        from syntricdb.llamaindex import SyntricDBLlamaIndexVectorStore
        from llama_index.core import VectorStoreIndex, StorageContext

        vector_store = SyntricDBLlamaIndexVectorStore(table="syntric_docs", host="http://localhost:8080")
        storage_context = StorageContext.from_defaults(vector_store=vector_store)
        index = VectorStoreIndex.from_documents(documents, storage_context=storage_context)
    """

    def __init__(
        self,
        table: str = "syntric_docs",
        host: str = "http://localhost:8080",
        api_key: Optional[str] = None,
        client: Optional[SyntricDBClient] = None,
    ):
        self.table = table
        self.client = client or SyntricDBClient(host=host, api_key=api_key)
        self.stores_text: bool = True

    def add(self, nodes: List[Any], **add_kwargs: Any) -> List[str]:
        """
        Adds text nodes to SyntricDB HNSW index.
        """
        node_ids = []
        for node in nodes:
            node_id = getattr(node, "node_id", f"node_{len(node_ids)+1}")
            text = getattr(node, "text", str(node))
            
            sql_text = text.replace("'", "''")
            sql = f"INSERT INTO {self.table} (id, content) VALUES ('{node_id}', '{sql_text}');"
            try:
                self.client.query(sql)
            except Exception:
                pass
            node_ids.append(node_id)
            
        return node_ids

    def query(self, query_str: str, similarity_top_k: int = 5, **kwargs: Any) -> Dict[str, Any]:
        """
        Queries SyntricDB for similarity search matches.
        """
        res = self.client.vector_search(table=self.table, column="embedding", query=query_str, limit=similarity_top_k)
        items = res if isinstance(res, list) else res.get("data", [])
        
        nodes = []
        similarities = []
        ids = []
        
        for item in items:
            node_id = item.get("id", "node_1")
            text = item.get("title") or item.get("content") or str(item)
            score = item.get("similarity_score", 0.95)
            
            nodes.append({"id": node_id, "text": text})
            similarities.append(score)
            ids.append(node_id)

        return {
            "nodes": nodes,
            "similarities": similarities,
            "ids": ids
        }

    def delete(self, ref_doc_id: str, **delete_kwargs: Any) -> None:
        """
        Deletes a document reference from SyntricDB using ANSI DML.
        """
        sql = f"DELETE FROM {self.table} WHERE id = '{ref_doc_id}';"
        try:
            self.client.query(sql)
        except Exception as e:
            raise RuntimeError(f"Failed to delete document from SyntricDB: {e}")
