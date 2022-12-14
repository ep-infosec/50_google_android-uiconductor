// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.uicd.backend.core.config;

import static com.google.common.base.StandardSystemProperty.USER_DIR;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.uicd.backend.core.constants.OCREngineType;
import com.google.uicd.backend.core.exceptions.UicdException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Global config class for Uicd core The variables such as DB connections and test output home dir
 * need to be set before use uicd core
 */
public class UicdConfig {

  protected UicdConfig() {
    // Exists only to defeat instantiation.
  }

  private static final Logger logger = LogManager.getLogManager().getLogger("uicd");
  private static UicdConfig instance = new UicdConfig();

  private static final String USERNAME_KEYWORD = "username";
  private static final String UICD_BASE_PATH_KEYWORD = "uicdbasepath";
  private static final String MYSQLPORT_KEYWORD = "mysqlport";
  private static final String MYSQL_CONNECTION_STRING_KEYWORD = "mysqlconnectionstr";
  private static final String XML_DUMPER_APK_VERSION_KEYWORD = "xmldumperversion";
  private static final String CONFIG_SPLITTER = "=";
  private static final String XML_DUMPER_APK_FOLDER_NAME = "xmldumper_apks/";
  private static final String OUTPUT_FOLDER_NAME = "output/";
  private static final String INPUT_FOLDER_NAME = "input/";
  private static final String TMP_FOLDER_NAME = "tmp/";
  private static final String UICD_DEP_FOLDER_PATH = "deps";
  private static final String ADB_FORWARD_START_PORT = "adb_forward_start_port";
  private static final String LOG_OUTPUT_LEVEL = "log_output_level";
  private static final String ADB_PATH = "adb_path";
  private static final String XMLDUMPER_PACKAGE_PREFIX = "xmldumper_package_prefix";
  private static final String REFERENCE_IMAGE_STORAGE = "reference_image_storage";
  private static final String UICD_LOCAL_MODE = "uicd_local_mode";
  private static final String UICD_OCR_ENGINE_TYPE = "uicd_ocr_engine_type";

  private static final String UICD_DISABLE_XML_DUMPER_FLAG = "disable_xml_dumper";
  private static final String UICD_ENABLE_MINICAP_FLAG = "enable_minicap";

  private String adbShellPath = "adb";
  private String currentUser = System.getProperty("user.name");
  private static final String UICD_DEFAULT_BASE_PATH = USER_DIR.value() + "/uicd-basepath";

  // In MobileHarness, each thread will have different BasePath, however in spring, we set the
  // basePath in the main thread(child thread generated by spring automatically).
  private final HashMap<Long, String> uicdBasePathThreadMap = new HashMap<>();
  private int mysqlPort = 3308;
  private String mysqlConnectionString = "";
  private int adbForwardStartPort = -1;
  private Level logLevel = Level.INFO;
  private String xmlDumperApkVersion = "3.1.2";
  // For internal version localMode will be overwrite to false in start.sh when start UICD. Open
  // sourced version will have run in local mode as default.
  private boolean localMode = true;
  private boolean disableXMLDumper = false;
  private boolean enableMinicap = false;

  // Default package name, can be override in the config file. In open source version of Uicd, it is
  // using a different package name.
  private String xmldumperPackagePrefix = "com.google.uicd.xmldumper";

  private String referenceImageStorage = "local";

  private OCREngineType orcEngineType = OCREngineType.DISABLE;

  public static UicdConfig getInstance() {
    return instance;
  }

  /* Get the adb shell path. */
  public String getAdbShellPath() {
    return adbShellPath;
  }

  public void setAdbShellPath(String adbShellPath) {
    this.adbShellPath = adbShellPath;
  }

  public String getDBConnStr() {
    if (!Strings.isNullOrEmpty(mysqlConnectionString)) {
      return mysqlConnectionString;
    }
    return "jdbc:mysql://localhost:"
        + getMysqlPort()
        + "/yuidb?autoReconnect=true&user=root&password=a667F407DE7C6AD07358FA&useUnicode=true"
        + "&characterEncoding=UTF-8&charset=utf8mb4&collation=utf8mb4_unicode_ci";
  }

  public synchronized String getBaseFolder() {
    if (uicdBasePathThreadMap.isEmpty()) {
      return UICD_DEFAULT_BASE_PATH;
    }
    if (uicdBasePathThreadMap.containsKey(Thread.currentThread().getId())) {
      return uicdBasePathThreadMap.get(Thread.currentThread().getId());
    }
    // In the Uicd local App mode, the UicdConfig object is set in the main thread. Each request
    // from the frontend is in a different thread. We should just use the basePath from the main
    // thread.
    return uicdBasePathThreadMap.values().stream().findFirst().get();
  }

  // used by uicd driver
  public synchronized void setBaseFolder(String uicdDataPath) {
    uicdBasePathThreadMap.put(Thread.currentThread().getId(), uicdDataPath);
  }

  /* Home folder for uicd output files */
  public String getTestOutputFolder() {
    return Paths.get(getBaseFolder(), OUTPUT_FOLDER_NAME).toString();
  }

  /* Home folder for uicd tmp files */
  public String getTestTmpFolder() {
    return Paths.get(getBaseFolder(), TMP_FOLDER_NAME).toString();
  }

  /* Home folder for uicd input files */
  public String getTestInputFolder() {
    return Paths.get(getBaseFolder(), INPUT_FOLDER_NAME).toString();
  }

  public Path getXmlDumperAPKPath() {
    return Paths.get(getBaseFolder(), UICD_DEP_FOLDER_PATH, XML_DUMPER_APK_FOLDER_NAME);
  }

  public String getDepsFolder() {
    return Paths.get(getBaseFolder(), UICD_DEP_FOLDER_PATH).toString();
  }

  public synchronized void parseConfigFile(Reader fileContents) throws IOException {
    HashMap<String, String> configVars = new HashMap<>();
    String line;
    BufferedReader bufferedReader = new BufferedReader(fileContents);
    while ((line = bufferedReader.readLine()) != null) {
      List<String> strs = Splitter.on(CONFIG_SPLITTER).splitToList(line);
      if (strs.size() >= 2) {
        configVars.put(
            Ascii.toLowerCase(strs.get(0)),
            strs.stream().skip(1).collect(Collectors.joining(CONFIG_SPLITTER)));
      }
    }
    if (configVars.containsKey(USERNAME_KEYWORD)) {
      this.currentUser = configVars.get(USERNAME_KEYWORD);
    }
    if (configVars.containsKey(UICD_BASE_PATH_KEYWORD)) {
      uicdBasePathThreadMap.put(
          Thread.currentThread().getId(), configVars.get(UICD_BASE_PATH_KEYWORD));
    }
    if (configVars.containsKey(MYSQLPORT_KEYWORD)) {
      this.mysqlPort = Integer.parseInt(configVars.get(MYSQLPORT_KEYWORD));
    }
    if (configVars.containsKey(MYSQL_CONNECTION_STRING_KEYWORD)) {
      this.mysqlConnectionString = configVars.get(MYSQL_CONNECTION_STRING_KEYWORD);
    }
    if (configVars.containsKey(LOG_OUTPUT_LEVEL)) {
      this.logLevel = Level.parse(configVars.get(LOG_OUTPUT_LEVEL));
    }
    if (configVars.containsKey(ADB_FORWARD_START_PORT)) {
      this.adbForwardStartPort = Integer.parseInt(configVars.get(ADB_FORWARD_START_PORT));
    }
    if (configVars.containsKey(XML_DUMPER_APK_VERSION_KEYWORD)) {
      this.xmlDumperApkVersion = configVars.get(XML_DUMPER_APK_VERSION_KEYWORD);
    }
    if (configVars.containsKey(ADB_PATH)) {
      this.adbShellPath = configVars.get(ADB_PATH);
    }
    if (configVars.containsKey(XMLDUMPER_PACKAGE_PREFIX)) {
      this.xmldumperPackagePrefix = configVars.get(XMLDUMPER_PACKAGE_PREFIX);
    }
    if (configVars.containsKey(REFERENCE_IMAGE_STORAGE)) {
      this.referenceImageStorage = configVars.get(REFERENCE_IMAGE_STORAGE);
    }
    if (configVars.containsKey(UICD_LOCAL_MODE)) {
      this.localMode = Boolean.parseBoolean(configVars.get(UICD_LOCAL_MODE));
    }
    if (configVars.containsKey(UICD_OCR_ENGINE_TYPE)) {
      this.orcEngineType = OCREngineType.fromString(configVars.get(UICD_OCR_ENGINE_TYPE));
    }
    if (configVars.containsKey(UICD_DISABLE_XML_DUMPER_FLAG)) {
      this.disableXMLDumper = Boolean.parseBoolean(configVars.get(UICD_DISABLE_XML_DUMPER_FLAG));
    }
    if (configVars.containsKey(UICD_ENABLE_MINICAP_FLAG)) {
      this.enableMinicap = Boolean.parseBoolean(configVars.get(UICD_ENABLE_MINICAP_FLAG));
    }
  }

  public void loadFromConfigFile(String cfgFilePath) throws UicdException {
    try {
      File file = new File(cfgFilePath);
      try (Reader fileReader = Files.newBufferedReader(file.toPath(), UTF_8)) {
        parseConfigFile(fileReader);
      }
    } catch (IOException e) {
      logger.warning("Cannot find uicd.cfg");
      throw new UicdException("Cannot find uicd.cfg");
    }
  }

  public String getCurrentUser() {
    return currentUser;
  }

  public Level getLogLevel() {
    return logLevel;
  }

  // required by mobileharness
  public void setLogLevel(Level logLevel) {
    this.logLevel = logLevel;
  }

  public int getAdbForwardStartPort() {
    return adbForwardStartPort;
  }

  public String getXmlDumperApkVersion() {
    return xmlDumperApkVersion;
  }

  public static void reset() {
    instance = new UicdConfig();
  }

  public int getMysqlPort() {
    return mysqlPort;
  }

  public String getXmldumperPackagePrefix() {
    return xmldumperPackagePrefix;
  }

  public void setReferenceImageStorage(String referenceImageStorage) {
    this.referenceImageStorage = referenceImageStorage;
  }

  public String getReferenceImageStorage() {
    return referenceImageStorage;
  }

  public boolean isLocalMode() {
    return localMode;
  }

  public OCREngineType getOrcEngineType() {
    return orcEngineType;
  }

  public boolean isDisableXMLDumper() {
    return disableXMLDumper;
  }

  public boolean isEnableMinicap() {
    return enableMinicap;
  }

  public void setEnableMinicap(boolean enableMinicap) {
    this.enableMinicap = enableMinicap;
  }
}
