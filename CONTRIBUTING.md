# Contributing to GPU Dev Accelerator Ultimate

Thank you for your interest in contributing! GPU Dev Accelerator Ultimate is free and open source (Apache License 2.0), and community contributions are welcome.

## How to Contribute

### Bug Reports

Found a bug? Help us improve!

1. Check if the issue already exists in our issue tracker
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Your environment (OS, IntelliJ version, plugin version)
   - Relevant logs or screenshots

### Feature Requests

Have an idea for a new feature?

1. Search existing feature requests
2. If your idea is new, create a feature request with:
   - Clear description of the feature
   - Use case and why it's valuable
   - Mockups or examples (if applicable)

### Code Contributions

Want to contribute code?

#### Community Contributors
- Fork the repository
- Create a feature branch
- Make your changes
- Submit a pull request

**Note:** Major features require discussion before implementation.

#### What We Accept
- ✅ Bug fixes
- ✅ Documentation improvements
- ✅ Example code and tutorials
- ✅ Test coverage improvements
- ✅ Performance optimizations
- ✅ Language support enhancements

#### What Needs Approval
- ⚠️ New features (please discuss first)
- ⚠️ Breaking changes
- ⚠️ Major refactoring

### Documentation

Documentation improvements are always welcome!

- Fix typos
- Clarify confusing sections
- Add examples
- Translate to other languages

## Development Setup

### Prerequisites
- JDK 17 or later
- IntelliJ IDEA 2023.2+
- Gradle 8.0+

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/gpudev/gpu-dev-accelerator-ultimate.git
   cd gpu-dev-accelerator-ultimate
   ```

2. **Build the plugin**
   ```bash
   ./gradlew build
   ```

3. **Run in IDE**
   ```bash
   ./gradlew runIde
   ```

4. **Run tests**
   ```bash
   ./gradlew test
   ```

### Project Structure

```
gpu-dev-accelerator-ultimate/
├── src/
│   ├── main/
│   │   ├── kotlin/com/gpudev/accelerator/
│   │   │   ├── cuda/              # CUDA language support
│   │   │   ├── metal/             # Metal language support
│   │   │   ├── opencl/            # OpenCL support
│   │   │   ├── debugger/          # Debugging tools
│   │   │   ├── profiler/          # Profiling tools
│   │   │   ├── actions/           # IDE actions
│   │   │   ├── settings/          # Settings UI
│   │   │   └── optimization/      # AI optimizer
│   │   └── resources/
│   │       ├── META-INF/plugin.xml
│   │       └── messages/
│   └── test/
│       └── kotlin/
├── examples/                       # Example code
├── docs/                          # Documentation
├── build.gradle.kts
└── README.md
```

## Coding Standards

### Kotlin Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable names

### Code Quality
- Write unit tests for new features
- Ensure all tests pass before submitting PR
- No compiler warnings
- Use IntelliJ's code formatter

### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add support for CUDA 12.0
fix: resolve memory leak in profiler
docs: update getting started guide
test: add tests for Metal parser
refactor: simplify kernel analysis
```

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### Manual Testing
1. Build plugin: `./gradlew buildPlugin`
2. Install from disk in IntelliJ
3. Test your changes
4. Verify on all supported platforms

## Pull Request Process

1. **Before starting:**
   - Discuss major changes in an issue first
   - Check if similar PR already exists

2. **Making changes:**
   - Create a feature branch from `main`
   - Write clear, concise commits
   - Add tests for new functionality
   - Update documentation

3. **Submitting:**
   - Ensure all tests pass
   - Update CHANGELOG.md
   - Write clear PR description
   - Reference related issues

4. **Review process:**
   - Maintainers will review your PR
   - Address feedback promptly
   - PR must be approved by at least one maintainer

5. **After merge:**
   - Your contribution will be included in next release
   - You'll be credited in CONTRIBUTORS.md

## Community Guidelines

### Be Respectful
- Treat everyone with respect
- Welcome newcomers
- No harassment or discrimination
- Keep discussions professional

### Communication Channels
- **GitHub Issues:** Bug reports and features
- **GitHub Discussions:** General questions
- **Discord:** Community chat (link in README)
- **Email:** For private matters

### Getting Help
- Check documentation first
- Search existing issues
- Ask in Discord community
- Create a GitHub issue

## Recognition

### Contributors
- Listed in CONTRIBUTORS.md
- Mentioned in release notes

### Top Contributors
- Special recognition
- Beta access to new features
- Input on roadmap decisions

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0, the same license as the project.

---

## Questions?

- **General:** community@gpudev.com
- **Security:** security@gpudev.com

Thank you for contributing to GPU Dev Accelerator Ultimate! 🚀
