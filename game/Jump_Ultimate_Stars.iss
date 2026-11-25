
#define MyAppName "JumpUltimateStars"
#define MyAppVersion "1.1.0"
#define MyAppExeName "JumpUltimateStars.exe"

[Setup]
AppId={{A82D7C51-5F42-4C79-9E31-7A0C8D61B420}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
OutputDir=D:\GameDev\Jump_Ultimate_Stars
OutputBaseFilename=JumpUltimateStars-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
UninstallDisplayIcon={app}\{#MyAppExeName}

[Tasks]
Name: "desktopicon"; Description: "Tạo biểu tượng ngoài Desktop"; GroupDescription: "Tùy chọn:"

[Files]
Source: "{#SourcePath}lwjgl3\build\construo\winX64\roast\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Khởi chạy JumpUltimateStars"; Flags: postinstall nowait skipifsilent
