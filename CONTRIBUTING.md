# Contributing to RemoteDexter

## Welcome

Thank you for your interest in contributing to RemoteDexter! This document outlines the development process, coding standards, and contribution guidelines for the project.

## Development Philosophy

RemoteDexter follows a **block-based development model** where features are implemented as self-contained, testable units. This ensures:

- **Modularity**: Features can be developed, tested, and deployed independently
- **Stability**: Core functionality remains unaffected by new features
- **Auditability**: Each change has clear scope and impact
- **Reversibility**: Features can be disabled or removed if needed

## Getting Started

### Prerequisites
- Go 1.21+ for desktop components
- Android Studio for mobile components
- Git for version control
- Basic understanding of cryptography and network security

### Development Setup
1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/remotedexter.git`
3. Set up development environment:
   ```bash
   cd remotedexter
   # Desktop setup
   cd src/desktop
   go mod download

   # Android setup
   cd ../mobile/android
   ./gradlew build
   ```
4. Run tests: `./validate-system.sh`

## Development Model

### Block-Based Development

All development happens in **blocks** - self-contained units of work:

#### Block Structure
```
block_name/
├── README.md           # Block specification and requirements
├── implementation/     # Source code changes
├── tests/             # Block-specific tests
└── docs/              # Documentation updates
```

#### Block Types
- **Feature Block**: New functionality
- **Enhancement Block**: Improve existing features
- **Security Block**: Security improvements
- **Infrastructure Block**: Build/test tooling
- **Documentation Block**: Documentation improvements

### Development Workflow

1. **Choose a Block**: Select from the roadmap or propose new blocks
2. **Create Block Branch**: `git checkout -b block/your-block-name`
3. **Implement**: Follow block specifications
4. **Test**: Run full validation suite
5. **Document**: Update relevant documentation
6. **Submit PR**: With block name and description

## Coding Standards

### Go (Desktop Components)

#### Code Style
- Follow standard Go formatting: `gofmt -w .`
- Use `go vet` and `golint` for code quality
- Maximum line length: 120 characters
- Use meaningful variable names

#### Security
- **No external dependencies** without security review
- **Zero-trust approach**: Validate all inputs
- **Key management**: Never log or expose cryptographic keys
- **Memory safety**: Use safe memory operations, avoid unsafe.Pointer

#### Error Handling
```go
// Good: Structured error handling
result, err := operation()
if err != nil {
    return fmt.Errorf("operation failed: %w", err)
}

// Bad: Silent failures
_ = operation()
```

#### Testing
- Unit tests for all public functions
- Integration tests for component interactions
- Security tests for cryptographic operations
- Memory leak tests for long-running operations

### Kotlin (Android Components)

#### Code Style
- Follow Kotlin coding conventions
- Use Android Studio's built-in formatter
- Maximum line length: 120 characters

#### Architecture
- MVVM pattern for UI components
- Repository pattern for data access
- Dependency injection with manual management (no external DI frameworks)

#### Security
- **Storage Access Framework** for file operations
- **Foreground services** for background operations
- **Permission requests** with clear user consent
- **Secure storage** for sensitive data

### Cross-Platform Consistency

#### Naming Conventions
- **Functions**: `CamelCase` (Go), `camelCase` (Kotlin)
- **Constants**: `SCREAMING_SNAKE_CASE`
- **Files**: `snake_case.go`, `CamelCase.kt`
- **Packages**: `lowercase` with clear hierarchy

#### API Design
- **Consistent parameter order**: context first, then specific parameters
- **Error handling**: Return errors, don't panic
- **Resource management**: Explicit cleanup with defer/Close()

## Block-Mode Development Rules

### Block Isolation
- **Single Responsibility**: Each block implements one feature
- **Clear Boundaries**: Define inputs, outputs, and dependencies
- **No Side Effects**: Don't modify unrelated code
- **Backward Compatible**: Don't break existing functionality

### Block Specification Format
```markdown
# Block: [Block Name]

## Objective
[Clear, measurable objective]

## Requirements
- [ ] Requirement 1
- [ ] Requirement 2

## Files Modified
- path/to/file1
- path/to/file2

## Testing
- [ ] Unit tests
- [ ] Integration tests
- [ ] Security tests

## Acceptance Criteria
- [ ] All requirements implemented
- [ ] All tests pass
- [ ] Documentation updated
- [ ] No performance regression
```

### Block Review Process
1. **Self-Review**: Ensure block meets all requirements
2. **Peer Review**: At least one maintainer review
3. **Security Review**: For security-related blocks
4. **Integration Testing**: Full system validation
5. **Merge**: Squash merge with clear commit message

## Security Expectations

### Security-First Development
- **Threat Modeling**: Consider attack vectors for all features
- **Input Validation**: Validate all external inputs
- **Cryptographic Review**: All crypto operations reviewed
- **Privacy Protection**: No unnecessary data collection

### Vulnerability Reporting
- **Responsible Disclosure**: Report security issues privately
- **Fix Timeline**: Critical issues fixed within 48 hours
- **Credit**: Security researchers credited in release notes

### Security Testing
- **Static Analysis**: Run security linters
- **Dynamic Analysis**: Fuzz testing for parsers
- **Dependency Scanning**: Regular dependency audits
- **Penetration Testing**: Annual security assessments

## Testing Requirements

### Test Coverage
- **Unit Tests**: 80%+ coverage for new code
- **Integration Tests**: End-to-end functionality
- **Performance Tests**: No regression in benchmarks
- **Security Tests**: Cryptographic operations validated

### Test Execution
```bash
# Run all tests
./validate-system.sh

# Run specific component tests
cd src/desktop && go test ./...

# Run Android tests
cd src/mobile/android && ./gradlew test
```

### Continuous Integration
- **Automated Testing**: All PRs run full test suite
- **Security Scanning**: Automated vulnerability detection
- **Performance Monitoring**: Benchmark comparisons
- **Code Quality**: Automated style and linting checks

## Documentation Requirements

### Code Documentation
- **Public APIs**: Full godoc comments
- **Complex Logic**: Inline comments explaining decisions
- **Security Considerations**: Document security assumptions

### User Documentation
- **README Updates**: For user-facing changes
- **API Documentation**: For developer-facing changes
- **Migration Guides**: For breaking changes

### Block Documentation
- **Block README**: Complete specification
- **Implementation Notes**: Design decisions and trade-offs
- **Testing Notes**: Test scenarios and edge cases

## Pull Request Process

### PR Template
```markdown
## Block: [Block Name]

### Description
[Brief description of changes]

### Files Changed
- [ ] src/path/to/file1
- [ ] src/path/to/file2

### Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Security tests pass

### Checklist
- [ ] Block specification followed
- [ ] No breaking changes
- [ ] Documentation updated
- [ ] Tests added/updated
```

### Review Requirements
- **Minimum Reviews**: 1 maintainer + 1 contributor
- **Security Blocks**: Additional security reviewer
- **Breaking Changes**: Architecture review required
- **New Dependencies**: Dependency review required

## Community Guidelines

### Communication
- **Respectful**: Be kind and constructive
- **Clear**: Use clear language and examples
- **Responsive**: Reply to issues and PRs promptly
- **Collaborative**: Work together toward shared goals

### Issue Management
- **Bug Reports**: Use bug report template
- **Feature Requests**: Use feature request template
- **Questions**: Use discussions for general questions
- **Security Issues**: Use private security advisory

### Release Process
- **Semantic Versioning**: Major.Minor.Patch
- **Changelog**: Clear description of changes
- **Security Updates**: Expedited release process
- **Beta Releases**: For major feature testing

## Recognition

Contributors are recognized through:
- **GitHub Contributors**: Listed in repository
- **Changelog Credits**: Mentioned in release notes
- **Security Hall of Fame**: For security researchers
- **Maintainer Status**: For significant contributions

## Getting Help

- **Documentation**: Check docs/ directory first
- **Issues**: Search existing issues
- **Discussions**: Use GitHub discussions for questions
- **Discord**: Join our community Discord (link in README)

---

Thank you for contributing to RemoteDexter! Your work helps build a more sovereign and secure computing future.