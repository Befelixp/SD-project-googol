param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("all", "gateway", "barrel", "downloader", "client")]
    [string]$Component
)

function Stop-JavaProcess {
    param(
        [string]$className
    )
    Write-Host "🛑 Procurando processos de $className..."
    $processes = Get-WmiObject Win32_Process -Filter "CommandLine LIKE '%$className%'" | Where-Object { $_.CommandLine -like '*java*' }
    
    if ($processes) {
        foreach ($process in $processes) {
            Write-Host "🔄 Parando processo $($process.ProcessId)..."
            Stop-Process -Id $process.ProcessId -Force
            Write-Host "✅ Processo parado com sucesso!"
        }
    } else {
        Write-Host "ℹ️ Nenhum processo de $className encontrado."
    }
}

switch ($Component) {
    "all" {
        Write-Host "🛑 Parando todos os processos..."
        Stop-JavaProcess "meta1sd.RMIGateway"
        Stop-JavaProcess "meta1sd.IndexStorageBarrel"
        Stop-JavaProcess "meta1sd.Downloader"
        Stop-JavaProcess "meta1sd.RMIClient"
    }
    "gateway" {
        Stop-JavaProcess "meta1sd.RMIGateway"
    }
    "barrel" {
        Stop-JavaProcess "meta1sd.IndexStorageBarrel"
    }
    "downloader" {
        Stop-JavaProcess "meta1sd.Downloader"
    }
    "client" {
        Stop-JavaProcess "meta1sd.RMIClient"
    }
} 