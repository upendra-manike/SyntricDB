package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
)

const SyntricDBURL = "syntricdb://admin:syntricdb_secret_pass@localhost:8080/default"

type QueryPayload struct {
	SQL      string `json:"sql"`
	Database string `json:"database"`
}

func parseConnectionURL(rawURL string) (string, string, string, string) {
	cleanURL := strings.Replace(rawURL, "jdbc:syntricdb://", "http://", 1)
	cleanURL = strings.Replace(cleanURL, "syntricdb://", "http://", 1)

	u, err := url.Parse(cleanURL)
	if err != nil {
		return "http://localhost:8080/api/sql", "", "", "default"
	}

	host := u.Hostname()
	port := u.Port()
	if port == "" {
		port = "8080"
	}
	apiURL := fmt.Sprintf("http://%s:%s/api/sql", host, port)

	user := ""
	pass := ""
	if u.User != nil {
		user = u.User.Username()
		pass, _ = u.User.Password()
	}

	db := strings.Trim(u.Path, "/")
	if db == "" {
		db = "default"
	}

	return apiURL, user, pass, db
}

func executeQuery(sql string) (string, error) {
	apiURL, user, pass, database := parseConnectionURL(SyntricDBURL)
	payload := QueryPayload{SQL: sql, Database: database}
	body, err := json.Marshal(payload)
	if err != nil {
		return "", err
	}

	req, err := http.NewRequest("POST", apiURL, bytes.NewBuffer(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json")

	if user != "" && pass != "" {
		auth := base64.StdEncoding.EncodeToString([]byte(user + ":" + pass))
		req.Header.Set("Authorization", "Basic "+auth)
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	return string(respBody), nil
}

func main() {
	fmt.Println("=================================================")
	fmt.Println("🔷 SyntricDB Go Integration Demo")
	fmt.Printf("🔗 Connection URL: %s\n", SyntricDBURL)
	fmt.Println("=================================================")

	createSQL := `
	CREATE TABLE go_metrics (
		id VARCHAR PRIMARY KEY,
		metric_name VARCHAR,
		value FLOAT,
		embedding FLOAT_VECTOR(128)
	);`
	res, err := executeQuery(createSQL)
	if err != nil {
		fmt.Printf("Error creating table: %v\n", err)
	} else {
		fmt.Printf("✅ Create Table Response: %s\n", res)
	}

	insertSQL := `
	INSERT INTO go_metrics VALUES (
		'met_601',
		'cpu_utilization',
		88.5,
		AI_EMBED('high cpu utilization server load alert')
	);`
	res, err = executeQuery(insertSQL)
	if err != nil {
		fmt.Printf("Error inserting record: %v\n", err)
	} else {
		fmt.Printf("✅ Insert Record Response: %s\n", res)
	}

	searchSQL := `
	SELECT id, metric_name, value 
	FROM go_metrics 
	WHERE embedding SIMILAR TO 'server cpu load alert' 
	TOP 1;`
	res, err = executeQuery(searchSQL)
	if err != nil {
		fmt.Printf("Error searching: %v\n", err)
	} else {
		fmt.Printf("\n🔍 Vector Search Results:\n%s\n", res)
	}

	fmt.Println("=================================================")
}
