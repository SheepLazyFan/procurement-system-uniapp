# Java Upgrade Plan

## Session Information

- **Session ID**: 20260302-001
- **Project Name**: procurement-server
- **Created**: 2026-03-02
- **Current Branch**: master
- **Current Commit**: 4cc3fde6a7f8073aacdaf07146779889721149ee
- **Working Branch**: appmod/java-upgrade-20260302-001

## Upgrade Goals

- **Java Runtime**: 1.8.0_151 → **Java 21 LTS**
- **Build Tool**: Set up Maven wrapper for consistent builds

## Current Technology Stack

| Dependency | Current Version | Target Version | Status | Notes |
|------------|----------------|----------------|---------|-------|
| Java | 1.8.0_151 | 21 | ⚠️ Major upgrade | LTS to LTS upgrade |
| Spring Boot | 3.2.3 | 3.2.3 | ✅ Compatible | Already Java 21 ready |
| MyBatis Plus | 3.5.5 | 3.5.5 | ✅ Compatible | Spring Boot 3 starter |
| MySQL Connector | Latest (managed) | Latest | ✅ Compatible | No changes needed |
| JWT (jjwt) | 0.12.5 | 0.12.5 | ✅ Compatible | Latest version |
| Knife4j | 4.4.0 | 4.4.0 | ✅ Compatible | Jakarta EE 9+ support |
| Lombok | Latest (managed) | Latest | ✅ Compatible | Java 21 compatible |
| EasyExcel | 3.3.3 | 3.3.3 | ✅ Compatible | No changes needed |

## Analysis

### Key Findings

1. **Current State**: Project is configured for Java 17 in pom.xml but running on Java 8
2. **Target State**: Java 21 LTS (already installed at: C:\Program Files\Java\jdk-21)
3. **Dependencies**: All dependencies are already Java 21 compatible (Spring Boot 3.2.3, Jakarta EE)
4. **Build Tool**: Maven not in PATH; Maven wrapper not present

### Derived Upgrades

**None required** - All dependencies in the current tech stack are already compatible with Java 21:
- Spring Boot 3.2.3 supports Java 21
- All Jakarta EE dependencies are compatible
- Third-party libraries (MyBatis Plus, JWT, Knife4j) are using Java 21 compatible versions

### Key Challenges

1. **Build Tool Setup**: Need to install Maven wrapper for consistent builds
2. **Environment Variables**: Need to switch JAVA_HOME from Java 8 to Java 21
3. **Compilation Verification**: Ensure both main and test code compile with Java 21
4. **Jakarta EE APIs**: Already using jakarta.* packages (correct for Java 21)

## Available Tools

| Tool | Version | Path | Notes |
|------|---------|------|-------|
| Java 8 | 1.8.0_151 | C:\Program Files\Java\jdk1.8.0_151 | Current (to be replaced) |
| Java 17 | 17.0.4 | C:\Program Files\Java\jdk-17.0.4 | Available |
| **Java 21** | 21 | C:\Program Files\Java\jdk-21 | **Target** |
| Maven | To be installed | ./mvnw | Will use wrapper |

## Upgrade Steps

### Step 1: Setup Environment & Maven Wrapper

**Description**: Install Maven wrapper to ensure consistent builds across environments

**Changes**:
- Download and install Maven wrapper (mvnw.cmd for Windows)
- Configure wrapper to use Maven 3.9.x (Java 21 compatible)

**Validation**:
```bash
./mvnw --version
```

**Expected Outcome**: Maven wrapper successfully installed and can be invoked

---

### Step 2: Setup Baseline

**Description**: Establish baseline with current configuration before making changes

**Changes**:
- Switch JAVA_HOME to Java 21 temporarily
- Attempt baseline compile with current pom.xml (java.version=17)
- Document any issues

**Validation**:
```bash
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
./mvnw clean test-compile
```

**Expected Outcome**: Document baseline compilation status; test failures acceptable at this point

---

### Step 3: Update Java Version to 21

**Description**: Update pom.xml to target Java 21

**Changes**:
- Update `<java.version>17</java.version>` to `<java.version>21</java.version>` in pom.xml
- Update compiler source/target if explicitly defined
- Commit changes

**Validation**:
```bash
./mvnw clean compile
```

**Expected Outcome**: Main source code compiles successfully with Java 21

---

### Step 4: Compile Test Code

**Description**: Verify test code compiles with Java 21

**Changes**:
- Fix any test compilation issues
- Update test dependencies if needed

**Validation**:
```bash
./mvnw clean test-compile
```

**Expected Outcome**: Both main and test code compile successfully

---

### Step 5: Final Validation

**Description**: Run all tests and verify complete upgrade success

**Changes**:
- Fix any test failures
- Verify all upgrade goals met
- Document final test results

**Validation**:
```bash
./mvnw clean test
```

**Expected Outcome**:
- ✅ All tests pass (100% pass rate)
- ✅ Java version: 21
- ✅ Compilation: Success (main + test)

---

## Options

- **Run tests before and after the upgrade**: true
- **Use intermediates**: false (not needed - all deps already compatible)
- **IDE Configuration**: Will need to update IDE to use Java 21 after upgrade

## Guidelines

- Minimal changes: Only update Java version configuration
- No dependency version updates unless required for Java 21 compatibility
- Use Maven wrapper for all build operations
- Test both compilation and runtime with Java 21
