param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("gateway", "barrel", "downloader", "client")]
    [string]$Component,
    
    [Parameter(Mandatory=$false)]
    [int]$ID = 1
)

# Verifica se o Java está instalado
try {
    $javaVersion = java -version 2>&1
    Write-Host "✅ Java encontrado: $javaVersion"
} catch {
    Write-Host "❌ Java não encontrado. Por favor, instale o Java e tente novamente."
    exit 1
}

# Verifica se o arquivo JAR existe
if (-not (Test-Path "../meta1sd.jar")) {
    Write-Host "❌ Arquivo meta1sd.jar não encontrado. Por favor, compile o projeto primeiro."
    exit 1
}

# Verifica se a pasta de configuração existe
if (-not (Test-Path "../config")) {
    Write-Host "❌ Pasta de configuração não encontrada em config"
    exit 1
}

$JAR_PATH = "../meta1sd.jar"
$LIBS_PATH = "../meta1sd/src/libs/jars/*"
$CONFIG_PATH = "../config"

# Função para verificar se o arquivo de configuração existe
function Test-ConfigFile {
    param($configFile)
    if (-not (Test-Path $configFile)) {
        Write-Host "❌ Arquivo de configuração não encontrado: $configFile"
        exit 1
    }
}

switch ($Component) {
    "gateway" {
        $configFile = "$CONFIG_PATH/gateway.properties"
        Test-ConfigFile $configFile
        Write-Host "🚀 Iniciando Gateway..."
        java -cp "$JAR_PATH;$LIBS_PATH" meta1sd.RMIGateway $configFile
    }
    "barrel" {
        $configFile = "$CONFIG_PATH/indexstoragebarrels.properties"
        Test-ConfigFile $configFile
        Write-Host "🚀 Iniciando Barrel $ID..."
        java -cp "$JAR_PATH;$LIBS_PATH" meta1sd.IndexStorageBarrel $ID $configFile
    }
    "downloader" {
        $configFile = "$CONFIG_PATH/downloaders.properties"
        Test-ConfigFile $configFile
        Write-Host "🚀 Iniciando Downloader..."
        java -cp "$JAR_PATH;$LIBS_PATH" meta1sd.Downloader $configFile
    }
    "client" {
        $configFile = "$CONFIG_PATH/client.properties"
        Test-ConfigFile $configFile
        Write-Host "🚀 Iniciando Cliente $ID..."
        java -cp "$JAR_PATH;$LIBS_PATH" meta1sd.RMIClient $ID $configFile
    }
} 