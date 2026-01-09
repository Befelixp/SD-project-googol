# SD Projeto Googol

### Alunos:

- Bernardo Pedro nº 2021231014
- João Matos nº 2021222748

## Estrutura do Projeto

```
.
└── src
    ├── config/             # Arquivos de configuração
    │   ├── client.properties
    │   ├── downloaders.properties
    │   ├── gateway.properties
    │   └── indexstoragebarrels.properties
    ├── data/               # Diretório de armazenamento de dados
    ├── libs/               # Bibliotecas externas
    │   └── jars/
    │       ├── gson-2.10.1.jar
    │       └── jsoup-1.17.2.jar
    ├── meta1sd/            # Código-fonte
    │   └── ...             # Arquivos fonte Java
    ├── scripts/            # Scripts utilitários
    │   └── ...             # Scripts de execução e gerenciamento
    └── target/             # Classes compiladas
```

## Pré-requisitos

- Java Development Kit (JDK)
- Prompt de Comando ou PowerShell (Windows)
- Bash shell (WSL/Linux) - Opcional

## Dependências

- Gson 2.10.1
- Jsoup 1.17.2

## Compilação e Execução

### Compilação e Execução no Windows

#### Compilar o Projeto

```cmd
cd src
javac -d target/meta1sd -cp "libs/jars/*" meta1sd/*.java
```

#### Executar Componentes

1. Executar Gateway

```cmd
java -cp "target/meta1sd;libs/jars/*" meta1sd.RMIGateway config/gateway.properties
```

2. Executar Barrel de Armazenamento de Índice (IBS)

```cmd
java -cp "target/meta1sd;libs/jars/*" meta1sd.IndexStorageBarrel <ID> config/indexstoragebarrels.properties
```

Substitua `<ID>` pelo ID desejado do barrel (por exemplo, 1 ou 2)

3. Executar Downloader

```cmd
java -cp "target/meta1sd;libs/jars/*" meta1sd.Downloader config/downloaders.properties
```

4. Executar Cliente

```cmd
java -cp "target/meta1sd;libs/jars/*" meta1sd.RMIClient <ID> config/client.properties
```

Substitua `<ID>` por um identificador de cliente

### Compilação e Execução no WSL/Linux

#### Scripts Disponíveis no Diretório `scripts/`

1. `compile.sh`

   - Compila todos os arquivos-fonte Java
   - Gera classes compiladas no diretório `target/meta1sd`

2. `runall.sh`

   - Inicia todos os componentes do sistema:
     - 1 Gateway
     - 1 Downloader
     - 2 Barrels de Armazenamento de Índice (IBS) com IDs 1 e 2

3. `runclient.sh`

   - Executa um novo cliente
   - Usa as configurações do arquivo `config/client.properties`

4. `rundownloader.sh`

   - Inicia um novo processo de downloader
   - Utiliza configurações do arquivo `config/downloaders.properties`

5. `rungateway.sh`

   - Inicializa uma nova gateway
   - Carrega configurações de `config/gateway.properties`

6. `runibs.sh <id>`

   - Cria um novo Barrel de Armazenamento de Índice (IBS)
   - Requer um ID como argumento
   - Usa configurações de `config/indexstoragebarrels.properties`

7. Scripts de Parada:
   - `stopall.sh`: Para todos os processos do sistema
   - `stopdownloader.sh`: Encerra todos os processos de download
   - `stopgateway.sh`: Desliga a gateway
   - `stopibs.sh`: Interrompe todos os barrels de armazenamento de índice

## Configuração

Edite os arquivos no diretório `config/` para personalizar:

- `client.properties`
- `downloaders.properties`
- `gateway.properties`
- `indexstoragebarrels.properties`

## Resolução de Problemas

- Verifique se o Java está instalado e o JAVA_HOME está configurado
- Verifique se o classpath inclui todos os arquivos JAR necessários
- Verifique as configurações de rede e portas
- Use caminhos completos se encontrar problemas com o classpath
- Certifique-se de que todos os scripts têm permissão de execução: `chmod +x *.sh`
