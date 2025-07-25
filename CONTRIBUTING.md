# Contributing to VFChatAI

Thank you for your interest in contributing to VFChatAI! This document provides guidelines for contributing to the project.

## 🚀 Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/your-username/VFChatAI.git
   cd VFChatAI
   ```
3. **Create a branch** for your feature or bug fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## 📋 Development Setup

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Node.js (for frontend development)

### Environment Setup
1. Copy `.env.example` to `.env` and fill in your credentials
2. Set up PostgreSQL database
3. Run the application with `mvn spring-boot:run`

## 🔧 Development Guidelines

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and single-purpose

### Frontend Guidelines
- Use modern JavaScript (ES6+)
- Follow consistent naming conventions
- Ensure responsive design
- Test across different browsers

### Database Changes
- Create migration scripts for schema changes
- Test migrations both up and down
- Document any new database relationships

## 🧪 Testing

### Backend Testing
```bash
mvn test
```

### Frontend Testing
- Test user interactions manually
- Verify responsive design on different screen sizes
- Check cross-browser compatibility

## 📝 Pull Request Process

1. **Update documentation** if needed
2. **Test your changes** thoroughly
3. **Create a pull request** with:
   - Clear title describing the change
   - Detailed description of what was changed and why
   - Screenshots for UI changes
   - Test results

### Pull Request Template
```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Refactoring

## Testing
- [ ] Tests pass locally
- [ ] Manual testing completed
- [ ] Cross-browser testing (if applicable)

## Screenshots (if applicable)
Add screenshots of any UI changes
```

## 🐛 Bug Reports

When reporting bugs, please include:
- Operating system and version
- Java version
- Browser (for frontend issues)
- Steps to reproduce
- Expected vs actual behavior
- Error logs or stack traces

## 💡 Feature Requests

For new features:
- Explain the use case
- Describe the proposed solution
- Consider backwards compatibility
- Discuss potential implementation approaches

## 📚 Documentation

- Update README.md for significant changes
- Add JSDoc comments for new functions
- Update API documentation if applicable
- Include examples for new features

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/example/VF_ChatAi/
│   │   ├── controller/     # REST endpoints
│   │   ├── service/        # Business logic
│   │   ├── repository/     # Data access
│   │   ├── model/          # JPA entities
│   │   └── security/       # Security config
│   └── resources/
│       ├── static/         # Frontend files
│       └── application.yml # Configuration
└── test/                   # Test files
```

## 🤝 Community Guidelines

- Be respectful and inclusive
- Help newcomers get started
- Share knowledge and best practices
- Report any inappropriate behavior

## 📞 Getting Help

- Open an issue for bugs or questions
- Join discussions in GitHub Discussions
- Check existing issues before creating new ones

## 🎉 Recognition

Contributors will be recognized in:
- GitHub contributors list
- CHANGELOG.md for significant contributions
- README.md acknowledgments

Thank you for contributing to VFChatAI! 🚀
