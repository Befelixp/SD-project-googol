# SD Projeto Googol

### Alunos:

- Bernardo Pedro nº 2021231014
- João Matos nº 2021222748

## Estrutura do Projeto

```
.
├── meta1sd/                # Módulo de Backend
│   └── src/
│       ├── config/         # Arquivos de configuração
│       │   ├── client.properties
│       │   ├── downloaders.properties
│       │   ├── gateway.properties
│       │   └── indexstoragebarrels.properties
│       ├── data/           # Diretório de armazenamento de dados
│       ├── libs/           # Bibliotecas externas
│       │   └── jars/
│       │       ├── gson-2.10.1.jar
│       │       └── jsoup-1.17.2.jar
│       ├── meta1sd/        # Código-fonte
│       │   └── ...         # Arquivos fonte Java
│       ├── scripts/        # Scripts utilitários
│       │   └── ...         # Scripts de execução e gerenciamento
│       └── target/         # Classes compiladas
│
└── meta2sd/                # Módulo de Frontend (Web)
    ├── src/
    │   ├── main/
    │   │   ├── java/      # Código-fonte Java
    │   │   ├── resources/ # Recursos (templates, CSS, etc.)
    │   │   └── webapp/    # Arquivos web
    │   └── test/          # Testes
    ├── target/            # Arquivos compilados e gerados
    ├── pom.xml           # Configuração Maven
    └── .env              # Configurações de ambiente
```

## Pré-requisitos

- Java Development Kit (JDK)
- Maven
- Prompt de Comando ou PowerShell (Windows)
- Bash shell (WSL/Linux) - Opcional

## Dependências

### meta1sd
- Gson 2.10.1
- Jsoup 1.17.2

### meta2sd
- Spring Boot 3.2.3
- Thymeleaf
- Google Gemini API
- Hacker News API

## Compilação e Execução

### meta1sd

#### Compilação e Execução no Windows

#### Compilar o Projeto

```cmd
cd meta1sd/src
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

### meta2sd

#### Configuração Inicial

1. Crie um arquivo `.env` na raiz do projeto meta2sd com as seguintes variáveis:
```
GEMINI_API_KEY=sua_chave_api_aqui
```

#### Compilação e Execução

1. Compilar o projeto:
```cmd
cd meta2sd
# Se tiver o Maven instalado:
mvn clean install

# Se não tiver o Maven instalado, use o Maven Wrapper:
# No Windows:
./mvnw.cmd clean install
# No Linux/Mac:
./mvnw clean install
```

2. Executar a aplicação:
```cmd
# Se tiver o Maven instalado:
mvn spring-boot:run

# Se não tiver o Maven instalado, use o Maven Wrapper:
# No Windows:
./mvnw.cmd spring-boot:run
# No Linux/Mac:
./mvnw spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`

#### Gerar Documentação JavaDoc

```cmd
cd meta2sd
# Se tiver o Maven instalado:
mvn javadoc:javadoc

# Se não tiver o Maven instalado, use o Maven Wrapper:
# No Windows:
./mvnw.cmd javadoc:javadoc
# No Linux/Mac:
./mvnw javadoc:javadoc
```

A documentação será gerada em `target/reports/apidocs/`

## Funcionalidades

### meta1sd
- Sistema de indexação distribuído
- Download e processamento de páginas web
- Busca por termos e URLs
- Armazenamento distribuído de índices

### meta2sd
- Interface web moderna e responsiva
- Busca por termos com análise de resultados via Gemini AI
- Busca por URLs que linkam para uma página específica
- Integração com Hacker News
- Paginação de resultados
- Cache de resultados para melhor performance

## Resolução de Problemas

### meta1sd
- Verifique se o Java está instalado e o JAVA_HOME está configurado
- Verifique se o classpath inclui todos os arquivos JAR necessários
- Verifique as configurações de rede e portas
- Use caminhos completos se encontrar problemas com o classpath
- Certifique-se de que todos os scripts têm permissão de execução: `chmod +x *.sh`

### meta2sd
- Verifique se o Maven está instalado corretamente
- Certifique-se de que o arquivo `.env` está configurado corretamente
- Verifique se a porta 8080 está disponível
- Verifique os logs da aplicação em caso de erros
- Certifique-se de que o meta1sd está rodando antes de iniciar o meta2sd
