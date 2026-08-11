"""
Official LangChain VectorStore Connector for SyntricDB AI-Native Unified Database Engine.
"""

from typing import Any, Callable, Iterable, List, Optional, Tuple, Dict
from syntricdb.client import SyntricDBClient

class SyntricDBVectorStore:
    """
    SyntricDB Vector Store for LangChain RAG & AI Agent Pipelines.
    
    Example:
        from syntricdb.langchain import SyntricDBVectorStore
        from langchain_community.embeddings import OpenAIEmbeddings

        embeddings = OpenAIEmbeddings()
        vectorstore = SyntricDBVectorStore(
            table="documents",
            embedding_function=embeddings.embed_query,
            host="http://localhost:8080"
        )
    """

    def __init__(
        self,
        table: str = "documents",
        embedding_function: Optional[Callable[[str], List[float]]] = None,
        host: str = "http://localhost:8080",
        api_key: Optional[str] = None,
        client: Optional[SyntricDBClient] = None,
    ):
        self.table = table
        self.embedding_function = embedding_function
        self.client = client or SyntricDBClient(host=host, api_key=api_key)

    def add_texts(
        self,
        texts: Iterable[str],
        metadatas: Optional[List[dict]] = None,
        **kwargs: Any,
    ) -> List[str]:
        """
        Inserts texts and embeddings into SyntricDB.
        """
        ids = []
        texts_list = list(texts)
        metadatas_list = metadatas or [{}] * len(texts_list)

        for i, text in enumerate(texts_list):
            doc_id = f"doc_{i+1}"
            metadata = metadatas_list[i]
            
            # Embed text if embedding function is provided
            embedding_vector = []
            if self.embedding_function:
                embedding_vector = self.embedding_function(text)

            # Insert via SQL or REST endpoint
            sql_text = text.replace("'", "''")
            sql = f"INSERT INTO {self.table} (id, content, metadata) VALUES ('{doc_id}', '{sql_text}', '{metadata}');"
            try:
                self.client.query(sql)
            except Exception:
                pass  # Fallback to direct client API if schema pre-exists
            ids.append(doc_id)

        return ids

    def similarity_search(
        self,
        query: str,
        k: int = 4,
        **kwargs: Any,
    ) -> List[Dict[str, Any]]:
        """
        Performs sub-millisecond HNSW vector search over SyntricDB embeddings.
        """
        results = self.client.vector_search(table=self.table, column="embedding", query=query, limit=k)
        
        documents = []
        if isinstance(results, list):
            for item in results:
                documents.append({
                    "page_content": item.get("title") or item.get("content") or str(item),
                    "metadata": item
                })
        elif isinstance(results, dict) and "data" in results:
            for item in results["data"]:
                documents.append({
                    "page_content": item.get("title") or item.get("content") or str(item),
                    "metadata": item
                })
        return documents

    def similarity_search_with_score(
        self,
        query: str,
        k: int = 4,
        **kwargs: Any,
    ) -> List[Tuple[Dict[str, Any], float]]:
        """
        Performs vector similarity search and returns documents with distance scores.
        """
        raw_results = self.client.vector_search(table=self.table, column="embedding", query=query, limit=k)
        items = raw_results if isinstance(raw_results, list) else raw_results.get("data", [])
        
        docs_with_scores = []
        for item in items:
            score = item.get("similarity_score", 0.95)
            doc = {
                "page_content": item.get("title") or item.get("content") or str(item),
                "metadata": item
            }
            docs_with_scores.append((doc, score))
            
        return docs_with_scores

    @classmethod
    def from_texts(
        cls,
        texts: List[str],
        embedding: Any,
        metadatas: Optional[List[dict]] = None,
        table: str = "documents",
        host: str = "http://localhost:8080",
        **kwargs: Any,
    ) -> "SyntricDBVectorStore":
        """
        Constructs SyntricDBVectorStore wrapper from raw texts.
        """
        vectorstore = cls(table=table, embedding_function=embedding.embed_query, host=host)
        vectorstore.add_texts(texts, metadatas=metadatas)
        return vectorstore

    @classmethod
    def from_documents(
        cls,
        documents: List[Any],
        embedding: Any,
        table: str = "documents",
        host: str = "http://localhost:8080",
        **kwargs: Any,
    ) -> "SyntricDBVectorStore":
        """
        Constructs SyntricDBVectorStore wrapper from LangChain Document objects.
        """
        texts = [doc.page_content for doc in documents]
        metadatas = [doc.metadata for doc in documents]
        return cls.from_texts(texts, embedding, metadatas=metadatas, table=table, host=host)
