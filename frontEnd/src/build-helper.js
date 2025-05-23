const { execSync } = require('child_process');
const path = require('path');

// npm config 변수 읽기
const buildDir = process.env.npm_package_config_reactBuildDir;
const springStaticDir = process.env.npm_package_config_springStaticResourcesDir;

if (!buildDir || !springStaticDir) {
    console.error('Error: Config variables (reactBuildDir, springStaticResourcesDir) are not set.');
    process.exit(1);
}

// 경로 조합 (frontend 폴더 기준)
const springStaticResourcesPath = path.resolve(__dirname, '..', springStaticDir); // springStaticDir이 '../src/...' 형태이므로
const targetStaticSubdir = path.join(springStaticResourcesPath, 'static');
const sourceBuildStaticDir = path.join(buildDir, 'static');

const commands = [
    // 1. Clean static subdirectory in Spring project
    `npx rimraf "${targetStaticSubdir}"`,
    // 2. Clean root assets in Spring project
    `npx shx rm -f "${path.join(springStaticResourcesPath, 'index.html')}" "${path.join(springStaticResourcesPath, 'favicon.ico')}" "${path.join(springStaticResourcesPath, 'manifest.json')}" "${path.join(springStaticResourcesPath, 'robots.txt')}" "${path.join(springStaticResourcesPath, 'asset-manifest.json')}" "${path.join(springStaticResourcesPath, '*.png')}"`,
    // 3. Copy static assets from React build to Spring's static/static
    `npx cpx "${sourceBuildStaticDir}/**/*" "${targetStaticSubdir}" --verbose`,
    // 4. Copy root assets from React build to Spring's static root
    `npx cpx "${buildDir}/{index.html,favicon.ico,manifest.json,robots.txt,asset-manifest.json,*.png,*.svg,*.jpg,*.jpeg}" "${springStaticResourcesPath}" --verbose`
];

try {
    for (const cmd of commands) {
        console.log(`\nExecuting: ${cmd}`);
        execSync(cmd, { stdio: 'inherit' });
    }
    console.log('\nAll build helper tasks completed successfully!');
} catch (error) {
    console.error('\nError during build helper tasks:', error);
    process.exit(1);
}