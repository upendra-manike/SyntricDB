using System;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace SyntricDBDemo
{
    class Program
    {
        private static readonly HttpClient client = new HttpClient();
        private const string SyntricDBUrl = "syntricdb://admin:syntricdb_secret_pass@localhost:8080/default";

        static async Task Main(string[] args)
        {
            Console.WriteLine("=================================================");
            Console.WriteLine("💜 SyntricDB C# / .NET 8 Integration Demo");
            Console.WriteLine($"🔗 Connection URL: {SyntricDBUrl}");
            Console.WriteLine("=================================================");

            string createSql = @"
                CREATE TABLE dotnet_events (
                    id VARCHAR PRIMARY KEY,
                    event_type VARCHAR,
                    severity VARCHAR,
                    embedding FLOAT_VECTOR(128)
                );";
            string res1 = await ExecuteQueryAsync(createSql);
            Console.WriteLine($"✅ Create Table Response: {res1}");

            string insertSql = @"
                INSERT INTO dotnet_events VALUES (
                    'evt_701',
                    'DatabaseConnectionTimeout',
                    'HIGH',
                    AI_EMBED('database connection pool timeout error failure')
                );";
            string res2 = await ExecuteQueryAsync(insertSql);
            Console.WriteLine($"✅ Insert Record Response: {res2}");

            string searchSql = @"
                SELECT id, event_type, severity 
                FROM dotnet_events 
                WHERE embedding SIMILAR TO 'connection timeout error' 
                TOP 1;";
            string res3 = await ExecuteQueryAsync(searchSql);
            Console.WriteLine($"\n🔍 Vector Search Results:\n{res3}");

            Console.WriteLine("=================================================");
        }

        private static async Task<string> ExecuteQueryAsync(string sql)
        {
            var (apiUrl, user, pass, database) = ParseConnectionUrl(SyntricDBUrl);
            var json = JsonSerializer.Serialize(new { sql = sql, database = database });
            var content = new StringContent(json, Encoding.UTF8, "application/json");

            var request = new HttpRequestMessage(HttpMethod.Post, apiUrl) { Content = content };

            if (!string.IsNullOrEmpty(user) && !string.IsNullOrEmpty(pass))
            {
                var authBytes = Encoding.UTF8.GetBytes($"{user}:{pass}");
                request.Headers.Authorization = new AuthenticationHeaderValue("Basic", Convert.ToBase64String(authBytes));
            }

            var response = await client.SendAsync(request);
            return await response.Content.ReadAsStringAsync();
        }

        private static (string apiUrl, string user, string pass, string database) ParseConnectionUrl(string urlStr)
        {
            var cleanUrl = urlStr.Replace("jdbc:syntricdb://", "http://").Replace("syntricdb://", "http://");
            var uri = new Uri(cleanUrl);
            var host = uri.Host;
            var port = uri.Port > 0 ? uri.Port : 8080;
            var apiUrl = $"http://{host}:{port}/api/sql";

            string user = "", pass = "";
            if (!string.IsNullOrEmpty(uri.UserInfo))
            {
                var parts = uri.UserInfo.Split(':');
                if (parts.Length > 0) user = Uri.UnescapeDataString(parts[0]);
                if (parts.Length > 1) pass = Uri.UnescapeDataString(parts[1]);
            }

            var db = uri.AbsolutePath.Trim('/');
            if (string.IsNullOrEmpty(db)) db = "default";

            return (apiUrl, user, pass, db);
        }
    }
}
