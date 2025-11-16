# Deployment Guide - GPU Dev Accelerator Ultimate

## Build & Distribution

### Local Build

```bash
# Build plugin
./gradlew clean build

# Output location
build/distributions/gpu-dev-accelerator-ultimate-1.0.0.zip
```

### Install Locally

1. Open IntelliJ IDEA
2. Go to `Settings/Preferences` → `Plugins`
3. Click gear icon ⚙️ → `Install Plugin from Disk...`
4. Select `build/distributions/gpu-dev-accelerator-ultimate-1.0.0.zip`
5. Restart IDE

### Run in Development

```bash
# Launch test IDE with plugin installed
./gradlew runIde
```

## JetBrains Marketplace Deployment

### Prerequisites

1. **JetBrains Account**
   - Create account at https://account.jetbrains.com
   - Join JetBrains Plugin Marketplace

2. **Plugin Repository**
   - Create plugin listing at https://plugins.jetbrains.com
   - Add description, screenshots, pricing

3. **Signing Certificate** (for paid plugins)
   ```bash
   # Generate private key
   openssl genrsa -out private_key.pem 4096
   
   # Generate certificate
   openssl req -new -x509 -key private_key.pem -out certificate.pem -days 3650
   
   # Set environment variables
   export CERTIFICATE_CHAIN=$(cat certificate.pem)
   export PRIVATE_KEY=$(cat private_key.pem)
   export PRIVATE_KEY_PASSWORD=your_password
   ```

4. **Publishing Token**
   ```bash
   # Get token from https://plugins.jetbrains.com/author/me/tokens
   export PUBLISH_TOKEN=your_token_here
   ```

### Publish Plugin

```bash
# Sign and publish
./gradlew signPlugin publishPlugin
```

### Manual Upload

1. Go to https://plugins.jetbrains.com/plugin/add
2. Upload `build/distributions/gpu-dev-accelerator-ultimate-1.0.0.zip`
3. Fill in:
   - Name: GPU Dev Accelerator Ultimate
   - Category: Tools Integration
   - Pricing Model: Freemium
   - Free trial: 30 days
   - Individual: $29.99/month or $299/year
   - Enterprise: Contact sales

4. Add screenshots and description
5. Submit for review

## Pricing Configuration

### JetBrains Marketplace Licensing

**Free Tier:**
- No payment required
- Limited features as defined in LicenseManager

**Individual License:**
- $29.99/month (paid monthly)
- $299/year (save 20%, paid annually)
- $199/year (educational discount - 33% off)

**Enterprise License:**
- Startup (1-10 seats): $49.99/seat/month or $499/year
- Team (11-50 seats): $99.99/seat/month or $999/year  
- Enterprise (51+ seats): Contact sales

### License Key Format

```
GPUDEV-[TYPE]-[RANDOM1]-[RANDOM2]

Examples:
- Free: No key needed
- Individual: GPUDEV-IND-A1B2C3-D4E5F6
- Enterprise: GPUDEV-ENT-G7H8I9-J0K1L2
```

### Generate Test License Keys

```kotlin
// For development/testing only
fun generateTestKey(type: String): String {
    val random1 = (1..6).map { ('A'..'Z') + ('0'..'9') }.flatten().shuffled().take(6).joinToString("")
    val random2 = (1..6).map { ('A'..'Z') + ('0'..'9') }.flatten().shuffled().take(6).joinToString("")
    return "GPUDEV-$type-$random1-$random2"
}

// Test keys
val individualKey = "GPUDEV-IND-TEST01-DEMO01"
val enterpriseKey = "GPUDEV-ENT-TEST02-DEMO02"
```

## Update Strategy

### Version Numbering

- Major: Breaking changes (e.g., 2.0.0)
- Minor: New features (e.g., 1.1.0)
- Patch: Bug fixes (e.g., 1.0.1)

### Release Process

1. **Update version** in `build.gradle.kts`:
   ```kotlin
   version = "1.1.0"
   ```

2. **Update CHANGELOG.md**:
   ```markdown
   ## [1.1.0] - 2024-12-15
   ### Added
   - Vulkan compute shader support
   - Enhanced AI optimization
   ```

3. **Build and test**:
   ```bash
   ./gradlew clean build
   ./gradlew test
   ./gradlew runIde  # Manual testing
   ```

4. **Commit and tag**:
   ```bash
   git commit -am "Release v1.1.0"
   git tag v1.1.0
   git push origin v1.1.0
   ```

5. **Publish**:
   ```bash
   ./gradlew publishPlugin
   ```

### Update Channels

**Stable** (default):
- Fully tested releases
- Recommended for all users

**Beta** (optional):
- Pre-release features
- For testing new functionality

**Nightly** (development):
- Daily builds
- For contributors only

## Monitoring & Analytics

### JetBrains Plugin Statistics

Available at https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID/edit

Metrics:
- Downloads
- Active users (DAU/MAU)
- Ratings and reviews
- Version distribution
- IDE version distribution

### Custom Analytics

Add to plugin for detailed tracking:

```kotlin
// In plugin initialization
class PluginAnalytics : ApplicationComponent {
    override fun initComponent() {
        // Track plugin activation
        // Track feature usage
        // Track errors and crashes
    }
}
```

### Crash Reporting

Integrate Sentry or similar:

```kotlin
dependencies {
    implementation("io.sentry:sentry:6.0.0")
}
```

## Support Infrastructure

### Documentation Site

Host at https://docs.gpudev.com

Structure:
- Getting Started
- API Reference
- Tutorials
- FAQ
- Troubleshooting

### Community Forum

Options:
- Discourse (self-hosted)
- GitHub Discussions
- Discord community
- Stack Overflow tag

### Support Email

Setup:
- support@gpudev.com (general)
- enterprise@gpudev.com (enterprise customers)
- security@gpudev.com (security issues)

### Issue Tracker

Use GitHub Issues:
- Bug reports
- Feature requests
- Questions

## Marketing Launch

### Pre-Launch Checklist

- [ ] Plugin tested on Windows, Mac, Linux
- [ ] Documentation complete
- [ ] Example projects ready
- [ ] Screenshots and demo video
- [ ] Press release written
- [ ] Social media accounts created
- [ ] Landing page live

### Launch Day

1. **8am PT:** Press release
2. **9am PT:** Hacker News post
3. **10am PT:** Reddit posts (r/CUDA, r/MachineLearning, r/gamedev)
4. **11am PT:** Twitter/X announcement
5. **12pm PT:** LinkedIn post
6. **2pm PT:** Email announcement
7. **3pm PT:** YouTube video

### Post-Launch

- Monitor support channels
- Respond to reviews
- Fix critical bugs immediately
- Engage with community

## Enterprise Sales

### CRM Setup

Use:
- HubSpot (free tier available)
- Salesforce
- Pipedrive

Track:
- Leads
- Opportunities
- Deals
- Customer communications

### Sales Process

1. **Lead Generation**
   - Inbound (website, marketplace)
   - Outbound (direct outreach)

2. **Qualification**
   - Company size
   - Use case
   - Budget
   - Timeline

3. **Demo**
   - Custom demo for their use case
   - Show ROI
   - Answer technical questions

4. **Pilot**
   - 30-day free trial
   - Dedicated support
   - Success metrics

5. **Negotiation**
   - Volume discounts
   - Payment terms
   - Training included

6. **Close**
   - Contract signed
   - License keys issued
   - Onboarding scheduled

### Enterprise Pricing

**Standard Pricing:**
- 1-10 seats: $499/seat/year
- 11-50 seats: $999/seat/year
- 51-100 seats: $1,499/seat/year
- 100+ seats: Custom pricing

**Volume Discounts:**
- 10+ seats: 10% off
- 50+ seats: 20% off
- 100+ seats: 30% off

**Contract Terms:**
- Annual contract
- Net 30 payment terms
- Auto-renewal option
- Support included

## Legal & Compliance

### Terms of Service

Required sections:
- License grant
- Restrictions
- Payment terms
- Refund policy
- Warranty disclaimer
- Limitation of liability

### Privacy Policy

Required for:
- User data collection
- Analytics
- GDPR compliance

### Security

For enterprise customers:
- Security whitepaper
- SOC 2 compliance (future)
- Data encryption
- Regular security audits

## Success Metrics

### Key Performance Indicators (KPIs)

**Product Metrics:**
- Downloads: Target 1,000/month
- Active users: Target 500 MAU
- Trial-to-paid: Target 5% conversion

**Financial Metrics:**
- MRR: Target $10K/month by Month 6
- ARR: Target $500K by Year 1
- CAC: Target < $500
- LTV: Target > $1,500

**Support Metrics:**
- Response time: < 24 hours
- Resolution time: < 48 hours
- Customer satisfaction: > 4.5/5

## Troubleshooting Common Issues

### Plugin Not Loading

**Issue:** Plugin fails to load in IDE

**Solutions:**
1. Check IntelliJ version compatibility
2. Verify plugin.xml is valid
3. Check for conflicting plugins
4. Review IDE logs

### License Activation Fails

**Issue:** User can't activate license

**Solutions:**
1. Verify license key format
2. Check server connectivity
3. Validate license hasn't expired
4. Contact support with details

### Performance Issues

**Issue:** Plugin slows down IDE

**Solutions:**
1. Profile plugin performance
2. Optimize heavy operations
3. Use background threads
4. Cache expensive computations

---

## Contact

**Technical Support:** support@gpudev.com  
**Enterprise Sales:** enterprise@gpudev.com  
**Security Issues:** security@gpudev.com  
**General Inquiries:** info@gpudev.com

**Website:** https://gpudev.com  
**Documentation:** https://docs.gpudev.com  
**GitHub:** https://github.com/gpudev/gpu-dev-accelerator-ultimate
