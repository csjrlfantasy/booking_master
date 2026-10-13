$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location "D:\AIProjects\BookingApi"
Write-Host "Java version:"
java -version
Write-Host "`nStarting Maven compile:"
& "D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd" clean compile
Write-Host "`nCompile finished. Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")