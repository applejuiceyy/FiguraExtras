@echo off
echo ::total value=4
echo ::progress value=1
powershell -Command "Invoke-WebRequest https://github.com/GrandpaScout/FiguraRewriteVSDocs/archive/master.zip -OutFile __docs.zip" > NUL
echo ::progress value=2
powershell -Command "Expand-Archive __docs.zip -DestinationPath __docs" > NUL
echo ::progress value=3
MOVE "%cd%\__docs\FiguraRewriteVSDocs-latest\src\.vscode" "%cd%\.vscode" > NUL
MOVE "%cd%\__docs\FiguraRewriteVSDocs-latest\src\*" "%cd%" > NUL
echo ::progress value=4
RMDIR /s /q "./__docs" > NUL
DEL /q "./__docs.zip" > NUL