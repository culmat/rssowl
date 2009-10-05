/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2003-2009 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License 1.0 which accompanies this     **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl - initial API and implementation (bpasero@rssowl.org)         **
 **                                                                          **
 **  **********************************************************************  */

/**
 * The NSIS-Script to create the RSSOwl installer.
 * 
 * @author bpasero
 * @version 2.0
 */

;#####	 Variables	######
!define VER_DISPLAY "2.0.0"

;#####   Include Modern UI   ######
!include "MUI.nsh"

;#####   Installer Configuration   ######
Name "RSSOwl 2.0"
OutFile "RSSOwl Setup.exe"
InstallDir $PROGRAMFILES\RSSOwl
InstallDirRegKey HKCU "Software\RSSOwl" ""
AllowRootDirInstall true
BrandingText " "
SetCompressor /SOLID lzma

;#####   Variables   ######
Var STARTMENU_FOLDER
Var MUI_TEMP

;#####   Functions   ######
Function "ExecRSSOwl"
  SetOutPath $INSTDIR
  Exec "$INSTDIR\rssowl.exe"
FunctionEnd

;#####	Version Information	######
VIProductVersion "${VER_DISPLAY}.0"
VIAddVersionKey "ProductName" "RSSOwl"
VIAddVersionKey "CompanyName" "RSSOwl Team"
VIAddVersionKey "LegalCopyright" "Benjamin Pasero"
VIAddVersionKey "FileDescription" "RSSOwl"
VIAddVersionKey "FileVersion" "${VER_DISPLAY}"

;#####   Interface Settings   ######
!define MUI_ABORTWARNING
!define MUI_UNABORTWARNING
!define MUI_ICON "res\win-install.ico"
!define MUI_UNICON "res\win-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP "res\header.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "res\welcome.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "res\welcome.bmp"

;### Remember language ###
!define MUI_LANGDLL_REGISTRY_ROOT "HKCU"
!define MUI_LANGDLL_REGISTRY_KEY "Software\RSSOwl"
!define MUI_LANGDLL_REGISTRY_VALUENAME "Installer Language"


;#####   Pages   ######

;### Welcome ###
!insertmacro MUI_PAGE_WELCOME

;### License ###
!insertmacro MUI_PAGE_LICENSE "res\epl.rtf"

;### Directory ###
!insertmacro MUI_PAGE_DIRECTORY

;### Startmenu ###
!define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU"
!define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\RSSOwl"
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"
!insertmacro MUI_PAGE_STARTMENU Application $STARTMENU_FOLDER

;### Install Status ###
!insertmacro MUI_PAGE_INSTFILES

;### Finish ###
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_FUNCTION ExecRSSOwl
#!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\doc\tutorial\en\index.html"
!insertmacro MUI_PAGE_FINISH

;### Uninstall ###
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

;#####   Languages   ######
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "German"
!insertmacro MUI_LANGUAGE "French"
!insertmacro MUI_LANGUAGE "Spanish"
!insertmacro MUI_LANGUAGE "Italian"
!insertmacro MUI_LANGUAGE "Dutch"
!insertmacro MUI_LANGUAGE "Danish"
!insertmacro MUI_LANGUAGE "Greek"
!insertmacro MUI_LANGUAGE "Russian"
!insertmacro MUI_LANGUAGE "PortugueseBR"
!insertmacro MUI_LANGUAGE "Norwegian"
!insertmacro MUI_LANGUAGE "Ukrainian"
!insertmacro MUI_LANGUAGE "Japanese"
!insertmacro MUI_LANGUAGE "SimpChinese"
!insertmacro MUI_LANGUAGE "Finnish"
!insertmacro MUI_LANGUAGE "Swedish"
!insertmacro MUI_LANGUAGE "Korean"
!insertmacro MUI_LANGUAGE "Polish"
!insertmacro MUI_LANGUAGE "TradChinese"
!insertmacro MUI_LANGUAGE "Hungarian"
!insertmacro MUI_LANGUAGE "Bulgarian"
!insertmacro MUI_LANGUAGE "Czech"
!insertmacro MUI_LANGUAGE "Slovenian"
!insertmacro MUI_LANGUAGE "Turkish"
!insertmacro MUI_LANGUAGE "Thai"
!insertmacro MUI_LANGUAGE "Serbian"
!insertmacro MUI_LANGUAGE "SerbianLatin"
!insertmacro MUI_LANGUAGE "Croatian"
!insertmacro MUI_LANGUAGE "Slovak"

Function .onInit
  !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

Function un.onInit
  !insertmacro MUI_UNGETLANGUAGE
FunctionEnd

;#####   Installer Section   ######
Section ""

  SetOutPath $INSTDIR
  File bin\*.*
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  SetOutPath $INSTDIR\configuration
  File bin\configuration\*.*
  
  # Features
  SetOutPath $INSTDIR\features\org.eclipse.rcp_3.4.200.R342_v20090122-989JESTEbig-SVaL8UJHcYBr4A63\META-INF
  File bin\features\org.eclipse.rcp_3.4.200.R342_v20090122-989JESTEbig-SVaL8UJHcYBr4A63\META-INF\*.*
  
  SetOutPath $INSTDIR\features\org.eclipse.rcp_3.4.200.R342_v20090122-989JESTEbig-SVaL8UJHcYBr4A63
  File bin\features\org.eclipse.rcp_3.4.200.R342_v20090122-989JESTEbig-SVaL8UJHcYBr4A63\*.*
  
  SetOutPath $INSTDIR\features\org.rssowl.dependencies_2.0.0
  File bin\features\org.rssowl.dependencies_2.0.0\*.*
  
  SetOutPath $INSTDIR\features\org.rssowl_2.0.0.200909212303
  File bin\features\org.rssowl_2.0.0.200909212303\*.*
  
  # Plugins
  SetOutPath $INSTDIR\plugins\org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731\META-INF
  File bin\plugins\org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731\META-INF\*.*
  
  SetOutPath $INSTDIR\plugins\org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731
  File bin\plugins\org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731\*.*
  
  SetOutPath $INSTDIR\plugins
  File bin\plugins\*.*
  
  ;### Startmenu ###
  !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    CreateDirectory "$SMPROGRAMS\$STARTMENU_FOLDER"
    CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\RSSOwl.lnk" "$INSTDIR\rssowl.exe" "" "$INSTDIR\rssowl.ico"
    CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
    CreateShortcut "$QUICKLAUNCH\RSSOwl.lnk" "$INSTDIR\rssowl.exe" "" "$INSTDIR\rssowl.ico"
  !insertmacro MUI_STARTMENU_WRITE_END
  
  WriteRegStr HKCU "Software\RSSOwl" "" $INSTDIR
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\RSSOwl" "DisplayName" "RSSOwl"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\RSSOwl" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\RSSOwl" "DisplayIcon" "$INSTDIR\rssowl.ico"

  WriteRegStr HKCR "feed" "" "URL:feed Protocol"
  WriteRegStr HKCR "feed" "URL Protocol" ""
  WriteRegStr HKCR "feed\DefaultIcon" "" "$\"$INSTDIR\rssowl.exe$\""
  WriteRegStr HKCR "feed\shell\open\command" "" "$\"$INSTDIR\rssowl.exe$\" $\"%1$\""
SectionEnd


;#####   Uninstaller Section   ######
Section "Uninstall"

  Delete "$INSTDIR\*.*"
  Delete "$INSTDIR\configuration\*.*"
  Delete "$INSTDIR\features\org.eclipse.rcp_3.4.200.R342_v20090122-989JESTEbig-SVaL8UJHcYBr4A63\META-INF\*.*"
  Delete "$INSTDIR\features\org.eclipse.rcp_3.4.200.R342_v20090122-989JESTEbig-SVaL8UJHcYBr4A63\*.*"
  Delete "$INSTDIR\features\org.rssowl.dependencies_2.0.0\*.*"
  Delete "$INSTDIR\features\org.rssowl_2.0.0.200909212303\*.*"
  Delete "$INSTDIR\features\*.*"
  Delete "$INSTDIR\plugins\org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731\META-INF\*.*"
  Delete "$INSTDIR\plugins\org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731\*.*"
  Delete "$INSTDIR\plugins\*.*"

  RMDir "$INSTDIR\configuration"
  RMDir "$INSTDIR\features\org.eclipse.rcp_3.4.200.R342_v20090122-989JESTEbig-SVaL8UJHcYBr4A63\META-INF"
  RMDir "$INSTDIR\features\org.eclipse.rcp_3.4.200.R342_v20090122-989JESTEbig-SVaL8UJHcYBr4A63"
  RMDir "$INSTDIR\features\org.rssowl.dependencies_2.0.0"
  RMDir "$INSTDIR\features\org.rssowl_2.0.0.200909212303"
  RMDir "$INSTDIR\features"
  RMDir "$INSTDIR\plugins\org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731\META-INF"
  RMDir "$INSTDIR\plugins\org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731"
  RMDir "$INSTDIR\plugins"
  RMDir "$INSTDIR"

  ;### Uninstall Startmenu ###
  !insertmacro MUI_STARTMENU_GETFOLDER Application $MUI_TEMP
  
  Delete "$SMPROGRAMS\$MUI_TEMP\Uninstall.lnk"
  Delete "$SMPROGRAMS\$MUI_TEMP\RSSOwl.lnk"
  Delete "$DESKTOP\RSSOwl.lnk"
  Delete "$QUICKLAUNCH\RSSOwl.lnk"

  ;Delete empty start menu parent diretories
  StrCpy $MUI_TEMP "$SMPROGRAMS\$MUI_TEMP"

  startMenuDeleteLoop:
  RMDir $MUI_TEMP
  GetFullPathName $MUI_TEMP "$MUI_TEMP\.."

  IfErrors startMenuDeleteLoopDone

  StrCmp $MUI_TEMP $SMPROGRAMS startMenuDeleteLoopDone startMenuDeleteLoop
  startMenuDeleteLoopDone:

  DeleteRegKey HKCU "Software\RSSOwl"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\RSSOwl"
  DeleteRegKey HKCR "feed"

SectionEnd