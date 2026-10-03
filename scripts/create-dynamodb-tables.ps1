#!/usr/bin/env pwsh
<#
  Script para criar tabelas DynamoDB no LocalStack (endpoint http://localhost:4566)
  BillingMode: PROVISIONED
  TableClass: STANDARD
  ReadCapacityUnits = 2, WriteCapacityUnits = 2

  Rode este script no PowerShell (Windows):
    pwsh .\scripts\create-dynamodb-tables.ps1
  Ou no Windows PowerShell (se tiver aws cli disponível):
    .\scripts\create-dynamodb-tables.ps1

  Observação: os comandos usam --endpoint-url http://localhost:4566 e --region us-east-1

  OPCIONAL: a aplicação já cria as tabelas sozinha na inicialização
  (aws.dynamodb.criar-tabelas=true, padrão). Use este script apenas quando
  essa opção estiver desativada.
#>

$endpoint = "http://localhost:4566"
$region = "us-east-1"

Write-Host "Criando tabela 'perfis'..."
aws dynamodb create-table `
  --table-name perfis `
  --attribute-definitions AttributeName=id,AttributeType=S AttributeName=tipo,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --global-secondary-indexes '[{"IndexName":"tipo-index","KeySchema":[{"AttributeName":"tipo","KeyType":"HASH"}],"Projection":{"ProjectionType":"ALL"},"ProvisionedThroughput":{"ReadCapacityUnits":2,"WriteCapacityUnits":2}}]' `
  --provisioned-throughput ReadCapacityUnits=2,WriteCapacityUnits=2 `
  --billing-mode PROVISIONED `
  --table-class STANDARD `
  --endpoint-url $endpoint --region $region --no-cli-pager

Write-Host "Criando tabela 'usuarios'..."
aws dynamodb create-table `
  --table-name usuarios `
  --attribute-definitions AttributeName=id,AttributeType=S AttributeName=email,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --global-secondary-indexes '[{"IndexName":"email-index","KeySchema":[{"AttributeName":"email","KeyType":"HASH"}],"Projection":{"ProjectionType":"ALL"},"ProvisionedThroughput":{"ReadCapacityUnits":2,"WriteCapacityUnits":2}}]' `
  --provisioned-throughput ReadCapacityUnits=2,WriteCapacityUnits=2 `
  --billing-mode PROVISIONED `
  --table-class STANDARD `
  --endpoint-url $endpoint --region $region --no-cli-pager

Write-Host "Criando tabela 'refresh_tokens'..."
aws dynamodb create-table `
  --table-name refresh_tokens `
  --attribute-definitions AttributeName=id,AttributeType=S AttributeName=token,AttributeType=S AttributeName=usuario_id,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --global-secondary-indexes '[{"IndexName":"token-index","KeySchema":[{"AttributeName":"token","KeyType":"HASH"}],"Projection":{"ProjectionType":"ALL"},"ProvisionedThroughput":{"ReadCapacityUnits":2,"WriteCapacityUnits":2}},{"IndexName":"usuario-index","KeySchema":[{"AttributeName":"usuario_id","KeyType":"HASH"}],"Projection":{"ProjectionType":"ALL"},"ProvisionedThroughput":{"ReadCapacityUnits":2,"WriteCapacityUnits":2}}]' `
  --provisioned-throughput ReadCapacityUnits=2,WriteCapacityUnits=2 `
  --billing-mode PROVISIONED `
  --table-class STANDARD `
  --endpoint-url $endpoint --region $region --no-cli-pager

Write-Host "Criando tabela 'auditoria_autenticacao'..."
aws dynamodb create-table `
  --table-name auditoria_autenticacao `
  --attribute-definitions AttributeName=id,AttributeType=S AttributeName=usuario_id,AttributeType=S AttributeName=data_hora,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --global-secondary-indexes '[{"IndexName":"usuario-index","KeySchema":[{"AttributeName":"usuario_id","KeyType":"HASH"},{"AttributeName":"data_hora","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"},"ProvisionedThroughput":{"ReadCapacityUnits":2,"WriteCapacityUnits":2}}]' `
  --provisioned-throughput ReadCapacityUnits=2,WriteCapacityUnits=2 `
  --billing-mode PROVISIONED `
  --table-class STANDARD `
  --endpoint-url $endpoint --region $region --no-cli-pager

Write-Host "Todas as tabelas foram solicitadas ao endpoint $endpoint. Use 'aws dynamodb list-tables' para verificar."

