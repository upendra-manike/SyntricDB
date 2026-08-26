<p align="center">
  <img src="./syntricdb_logo.png" alt="SyntricDB Logo" width="360" />
</p>

# ⚡ SyntricDB: Next-Generation AI-Native Unified Database Engine

[![Build Status](https://github.com/upendra-manike/SyntricDB/workflows/SyntricDB%20CI%20Workflow/badge.svg)](https://github.com/upendra-manike/SyntricDB/actions)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://jdk.java.net/21/)
[![YouTube Demo](https://img.shields.io/badge/YouTube-Watch%20Demo-red?logo=youtube)](https://www.youtube.com/watch?v=8xxpJwloe30)

> 🎬 **[Watch the Official SyntricDB Studio Video Demo on YouTube](https://www.youtube.com/watch?v=8xxpJwloe30)**

**SyntricDB** is a distributed, high-performance, AI-native unified database engine built from the ground up on **Java 21 LTS, Netty 4, HNSW Vector Indexing, LSM-Tree Storage, BM25 Full-Text Search, and Raft Consensus**.

Instead of stitching together PostgreSQL, Redis, Elasticsearch, Kafka, and Pinecone over high-latency network boundaries, **SyntricDB unifies SQL, Vector Search, In-Memory Caching, Streaming, Full-Text Search, and Built-In AI SQL Functions into ONE engine**.

---

## 📚 Guides, Documentation & Multi-Language Examples

- 💻 **[Multi-Language Code Examples Repository](https://github.com/upendra-manike/SyntricDB_Examples)**: Dedicated repo for **Spring Boot JPA, Python, Node.js, Go, C#, Rust, and cURL**.
- 🐍 **[Python Client (`pip install syntricdb-client`)](https://pypi.org/project/syntricdb-client/)**: Official PyPI Package.
- 💚 **[Node.js Client (`npm install syntricdb-client`)](https://www.npmjs.com/package/syntricdb-client)**: Official npm Registry Package.
- 🍃 **[Spring Boot & JPA Integration Example](https://github.com/upendra-manike/SyntricDB_Examples/tree/main/springboot-jpa)**: Connect via `@Entity`, `@Repository`, and `@Transactional`.
- 🐍 **[Python Integration Example](https://github.com/upendra-manike/SyntricDB_Examples/tree/main/python)**: Connect via REST API using Python `requests`.
- 💚 **[Node.js Integration Example](https://github.com/upendra-manike/SyntricDB_Examples/tree/main/nodejs)**: Connect via JavaScript async fetch.
- 🔷 **[Go Integration Example](https://github.com/upendra-manike/SyntricDB_Examples/tree/main/go)**: Connect via Golang `net/http`.
- 💜 **[C# / .NET 8 Integration Example](https://github.com/upendra-manike/SyntricDB_Examples/tree/main/csharp)**: Connect via C# `HttpClient`.
- 🦀 **[Rust Integration Example](https://github.com/upendra-manike/SyntricDB_Examples/tree/main/rust)**: Connect via Rust `tokio` and `reqwest`.
- 🤖 **[AI Agent Specification File (`llms.txt`)](./llms.txt)**: Standard machine-readable AI context file for LLMs & AI coding assistants.
- 🌟 **[Awesome Lists Submission Kit](./AWESOME_LISTS_SUBMISSION_KIT.md)**: PR templates for `awesome-java`, `awesome-vector-search`, and `awesome-database`.
- 🏷️ **[GitHub Metadata & AI SEO Guide](./GITHUB_REPOS_METADATA.md)**: Topic tags and SEO metadata optimization.
- 🎬 **[Official YouTube Video Walkthrough](https://www.youtube.com/watch?v=8xxpJwloe30)**: Watch the full feature demo.
- 🎓 **[Step-by-Step Interactive Tutorial](./TUTORIAL.md)**: Hands-on guide from zero to running AI vector queries.
- 📖 **[Complete Technical Documentation](./DOCUMENTATION.md)**: Full Query & Architecture Reference Guide.
- ☁️ **[AWS EC2 & Cloud Deployment Guide](./deploy/aws_ec2_install.sh)**: Enterprise production installation script.

---

## 🚀 Quickstart & One-Line Installers

### 🍏 macOS & Linux (One-Line Terminal Install)
```bash
curl -fsSL https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/mac/install_mac.sh | bash
```

### 🪟 Windows 10 / 11 & Windows Server (PowerShell or CMD)
**One-Line Online PowerShell Install** (Normal User or Administrator):
```powershell
powershell -ExecutionPolicy Bypass -Command "iwr -useb https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/windows/install_windows.ps1 | iex"
```
Or run locally via PowerShell or double-click `install.bat`:
```cmd
install.bat
```
*(Automatic fallback for non-admin users guarantees clean installation without permission errors!)*

### 🐳 Docker One-Liner (Cloud & Container Quickstart)
```bash
docker run -d -p 8080:8080 --name syntricdb ghcr.io/upendra-manike/syntricdb:latest
```

### 🐙 Docker Compose (Single-Node & 3-Node Raft Cluster)
```bash
# Single Node Production Deployment
docker-compose up -d

# 3-Node Distributed Raft Consensus Cluster
docker-compose -f docker-compose.cluster.yml up -d
```

### ☸️ Kubernetes & Helm Deployment
```bash
# Helm Chart Deploy
helm install syntricdb ./deploy/helm/syntricdb

# Standard Kubernetes Manifest Deploy
kubectl apply -f deploy/k8s/
```

### 🐧 AWS EC2 / Universal Cloud VM One-Click Deploy
```bash
curl -fsSL https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/cloud/deploy_cloud_docker.sh | bash
```

### 🏷️ GitHub Release Version Bumping
```bash
# Bump version, tag release v1.1.0 and push to trigger automated GitHub Release & GHCR build pipeline
./deploy/bump_version.sh 1.1.0
git push origin main --tags
```

---

## 🔑 Default Credentials & Access Points

- **Username**: `admin`
- **Password**: `syntricdb_secret_pass`
- **Connection URI**: `syntricdb://admin:syntricdb_secret_pass@localhost:8080/default`
- **Web Dashboard**: 👉 **[http://localhost:8080/](http://localhost:8080/)**
- **REST API**: `http://localhost:8080/api/sql`

---

## 🍃 Spring Boot & Spring Data JPA Example

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // Native SyntricDB Vector Search inside Spring Data Repository!
    @Query(value = "SELECT * FROM products WHERE category = :cat AND embedding SIMILAR TO :term TOP :limit", nativeQuery = true)
    List<Product> searchByVectorSimilarity(@Param("cat") String category, 
                                           @Param("term") String searchTerm, 
                                           @Param("limit") int limit);
}
```

---

## 📜 License

SyntricDB is open-source software licensed under the **[Apache License 2.0](./LICENSE)**.
