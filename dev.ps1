# 本地开发帮助脚本
# 用法:
#   .\dev.ps1                  显示帮助
#   .\dev.ps1 infra            启动 MySQL + Redis (Docker)
#   .\dev.ps1 install          安装前后端依赖
#   .\dev.ps1 backend          编译并启动后端 (9000)
#   .\dev.ps1 backend -SkipBuild   跳过编译，直接启动后端
#   .\dev.ps1 frontend         启动前端 Vite (8080)
#   .\dev.ps1 all              启动 infra，并在新窗口分别跑前后端

param(
    [Parameter(Position = 0)]
    [ValidateSet('help', 'infra', 'install', 'backend', 'frontend', 'all')]
    [string]$Action = 'help',

    [switch]$SkipBuild,
    [switch]$SkipInfra
)

$ErrorActionPreference = 'Stop'

# 控制台切 UTF-8，否则 Java 日志(UTF-8)在 GBK 代码页下会乱码
function Set-ConsoleUtf8 {
    try { chcp 65001 | Out-Null } catch {}
    $utf8 = [System.Text.UTF8Encoding]::new($false)
    [Console]::OutputEncoding = $utf8
    [Console]::InputEncoding = $utf8
    $global:OutputEncoding = $utf8
}
Set-ConsoleUtf8

$Root = $PSScriptRoot
$Singleton = Join-Path $Root 'singleton'
$Vue = Join-Path $Root 'vue'
$EnvFile = Join-Path $Root '.env'
$MvnSettings = Join-Path $Root '.vscode\maven-settings-cn.xml'
$JavaHome = 'C:\app\jdk\jdk-23.0.2'
$JvmEncodingArgs = '-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dfile.encoding=UTF-8'

function Show-Help {
    Write-Host @"

本地开发命令 (在仓库根目录执行)

  .\dev.ps1 infra              启动 MySQL + Redis (Docker)
  .\dev.ps1 install            安装前后端依赖 (npm ci + mvn package)
  .\dev.ps1 backend            编译并启动后端 -> http://localhost:9000/api
  .\dev.ps1 backend -SkipBuild 跳过编译，直接启动后端
  .\dev.ps1 frontend           启动前端 Vite -> http://localhost:8080
  .\dev.ps1 all                infra + 新窗口启动前后端

提示:
  - 首次使用前请配置根目录 .env
  - 后端日志中文乱码时脚本已自动设置 UTF-8 控制台编码

"@
}

function Import-DotEnv {
    if (-not (Test-Path $EnvFile)) {
        Write-Warning ".env 不存在，将使用 application-dev.yml 中的默认值"
        return
    }
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $idx = $line.IndexOf('=')
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        Set-Item -Path "env:$key" -Value $val
    }
}

function Initialize-JavaEnv {
    if (-not (Test-Path $JavaHome)) {
        throw "未找到 JDK: $JavaHome`n请安装 JDK 23 或修改 dev.ps1 中的 `$JavaHome"
    }
    $env:JAVA_HOME = $JavaHome
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $env:JAVA_TOOL_OPTIONS = $JvmEncodingArgs
    $env:MAVEN_OPTS = $JvmEncodingArgs
}

function Stop-DockerAppContainers {
    cmd /c "docker compose stop backend frontend >nul 2>&1"
}

function Free-Port {
    param([int]$Port)
    Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object {
            Write-Host ">>> 结束占用 ${Port} 端口的进程 PID $_"
            Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        }
}

function Start-Infra {
    Set-Location $Root
    Stop-DockerAppContainers
    Write-Host '>>> 启动 MySQL + Redis ...'
    docker compose up -d mysql redis
    if ($LASTEXITCODE -ne 0) { throw 'docker compose 启动失败' }
    Write-Host '>>> infra 已就绪'
}

function Install-Dependencies {
    Initialize-JavaEnv

    if (-not (Test-Path (Join-Path $Vue 'node_modules'))) {
        Write-Host '>>> 安装前端依赖 ...'
        Push-Location $Vue
        npm ci
        if ($LASTEXITCODE -ne 0) { throw 'npm ci 失败' }
        Pop-Location
    } else {
        Write-Host '>>> 前端 node_modules 已存在，跳过 npm ci'
    }

    Write-Host '>>> 编译后端 ...'
    Push-Location $Singleton
    mvn -s $MvnSettings clean package '-DskipTests' -pl application -am
    if ($LASTEXITCODE -ne 0) { throw 'mvn package 失败' }
    Pop-Location
    Write-Host '>>> 依赖安装完成'
}

function Start-Backend {
    Import-DotEnv
    Initialize-JavaEnv

    Set-Location $Root
    Stop-DockerAppContainers
    Free-Port -Port 9000

    Push-Location $Singleton
    if (-not $SkipBuild) {
        Write-Host '>>> 编译后端 ...'
        mvn -s $MvnSettings install '-DskipTests' -pl application -am
        if ($LASTEXITCODE -ne 0) { throw 'mvn install 失败' }
    } else {
        Write-Host '>>> 跳过编译，直接启动后端'
    }

    Write-Host '>>> 启动后端 http://localhost:9000/api'
    mvn -s $MvnSettings spring-boot:run -pl application "-Dspring-boot.run.jvmArguments=$JvmEncodingArgs"
    $code = $LASTEXITCODE
    Pop-Location
    exit $code
}

function Start-Frontend {
    if (-not (Test-Path (Join-Path $Vue 'node_modules'))) {
        Write-Host '>>> 未找到 node_modules，先执行 npm ci ...'
        Push-Location $Vue
        npm ci
        if ($LASTEXITCODE -ne 0) { throw 'npm ci 失败' }
        Pop-Location
    }

    Free-Port -Port 8080
    Push-Location $Vue
    Write-Host '>>> 启动前端 http://localhost:8080'
    npm run dev
    $code = $LASTEXITCODE
    Pop-Location
    exit $code
}

function Start-All {
    if (-not $SkipInfra) {
        Start-Infra
    }

    $self = $MyInvocation.MyCommand.Path
    $psArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $self)

    Write-Host '>>> 在新窗口启动后端 ...'
    Start-Process powershell.exe -ArgumentList ($psArgs + 'backend', '-SkipBuild')

    Write-Host '>>> 在新窗口启动前端 ...'
    Start-Process powershell.exe -ArgumentList ($psArgs + 'frontend')

    Write-Host @"

>>> 已启动前后端（各开一个 PowerShell 窗口）
    后端: http://localhost:9000/api
    前端: http://localhost:8080

"@
}

switch ($Action) {
    'help' { Show-Help }
    'infra' { Start-Infra }
    'install' { Install-Dependencies }
    'backend' { Start-Backend }
    'frontend' { Start-Frontend }
    'all' { Start-All }
}
