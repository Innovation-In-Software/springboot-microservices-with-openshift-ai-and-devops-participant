# Publish a TransactionSubmitted JSON file to the Lab 5 Kafka container.
param(
    [Parameter(Mandatory = $true)]
    [string]$File
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path $File)) {
    throw "Event file not found: $File"
}

Get-Content -Raw -Path $File | docker exec -i md287-lab5-kafka `
    /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:9092 `
    --topic transactions.submitted

Write-Host "Published $File to transactions.submitted"
