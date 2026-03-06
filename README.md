# ALL-IN-BD

A repository for storing ALL-IN-BD project files, configured with Git LFS to handle large PHP and JSON files exceeding GitHub's 100MB limit.

## Project Structure

```
.
├── admin/          # Admin panel files
├── assets/         # Static assets (images, CSS, JS)
├── Myrental/       # Rental module files
└── SERVER_COPY/    # Server configuration and backup files
```

## Git LFS Setup

This repository uses [Git LFS](https://git-lfs.github.com/) to track large files. The following file types are stored in LFS:

- `*.zip` — Archive files
- `*.json` — JSON data files
- `*.php` — PHP source files

### Getting Started

1. **Install Git LFS** on your machine:
   ```bash
   # macOS
   brew install git-lfs

   # Ubuntu/Debian
   sudo apt-get install git-lfs

   # Windows (via Chocolatey)
   choco install git-lfs
   ```

2. **Initialize Git LFS** in your local environment:
   ```bash
   git lfs install
   ```

3. **Clone the repository**:
   ```bash
   git clone https://github.com/TOM-X-420/MR.TOM.git
   ```

4. Git LFS will automatically download the large files tracked in `.gitattributes` when you clone or pull.

### Pushing Large Files

When adding new large files, Git LFS will handle them automatically based on the `.gitattributes` configuration:

```bash
git add yourfile.php
git commit -m "Add large PHP file"
git push origin main
```

### Verifying LFS Tracking

To check which files are tracked by Git LFS:

```bash
git lfs ls-files
```

## Requirements

- Git 2.x or higher
- Git LFS 2.x or higher
