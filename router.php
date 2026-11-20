<?php
// router.php - Serves static files from root, or routes API requests to backend/public/index.php

$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$filePath = __DIR__ . $uri;

// 1. If physical file exists (e.g. /apps/..., /index.html, /sequence/...), serve it directly
if ($uri !== '/' && file_exists($filePath) && !is_dir($filePath)) {
    return false; // Tells PHP built-in server to serve static file directly
}

// 2. Handle directory indices (e.g. /apps/demand-verification/ -> /apps/demand-verification/index.html)
if (is_dir($filePath) && file_exists(rtrim($filePath, '/') . '/index.html')) {
    include rtrim($filePath, '/') . '/index.html';
    exit;
}

// 3. Otherwise route API requests or root to the backend public router
include __DIR__ . '/backend/public/index.php';
