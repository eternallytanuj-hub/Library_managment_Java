/**
 * Online Library Management System - Flat UI & Canvas Charting Engine
 * Zero dependencies, pure native vanilla JavaScript.
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Auto-dismiss flash alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s ease';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 500);
        }, 5000);
    });

    // 2. Render Analytics Charts if on Reports page
    initReportsCharts();
});

/**
 * Renders HTML5 Canvas Bar, Donut, and Trend Charts without external libraries.
 */
function initReportsCharts() {
    const trendCanvas = document.getElementById('trendChart');
    const genreCanvas = document.getElementById('genreChart');
    const availCanvas = document.getElementById('availChart');

    if (!trendCanvas && !genreCanvas && !availCanvas) {
        return;
    }

    // Fetch JSON metrics from /librarian/reports?format=json
    fetch('reports?format=json')
        .then(res => res.json())
        .then(data => {
            if (trendCanvas && data.monthlyTrends) {
                renderBarChart(trendCanvas, data.monthlyTrends, '#2563eb');
            }
            if (genreCanvas && data.genres) {
                renderDonutChart(genreCanvas, data.genres);
            }
            if (availCanvas && data.availability) {
                renderPieChart(availCanvas, data.availability);
            }
        })
        .catch(err => {
            console.error('Failed to load chart metrics:', err);
        });
}

function renderBarChart(canvas, dataObj, color) {
    const ctx = canvas.getContext('2d');
    const labels = Object.keys(dataObj);
    const values = Object.values(dataObj);

    const w = canvas.width = canvas.parentElement.clientWidth;
    const h = canvas.height = canvas.parentElement.clientHeight || 260;

    ctx.clearRect(0, 0, w, h);

    if (labels.length === 0) {
        drawEmptyMessage(ctx, w, h);
        return;
    }

    const padding = 40;
    const chartW = w - padding * 2;
    const chartH = h - padding * 2;
    const maxVal = Math.max(...values, 5);

    const barW = Math.min(48, (chartW / labels.length) * 0.6);
    const gap = chartW / labels.length;

    // Draw baseline
    ctx.fillStyle = '#e2e8f0';
    ctx.fillRect(padding, h - padding, chartW, 2);

    labels.forEach((label, i) => {
        const val = values[i];
        const barH = (val / maxVal) * chartH;
        const x = padding + i * gap + (gap - barW) / 2;
        const y = h - padding - barH;

        // Bar
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.roundRect(x, y, barW, barH, [4, 4, 0, 0]);
        ctx.fill();

        // Value text
        ctx.fillStyle = '#0f172a';
        ctx.font = '600 11px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(val, x + barW / 2, y - 6);

        // Label text
        ctx.fillStyle = '#64748b';
        ctx.font = '500 11px sans-serif';
        ctx.fillText(label, x + barW / 2, h - padding + 18);
    });
}

function renderDonutChart(canvas, dataObj) {
    const ctx = canvas.getContext('2d');
    const labels = Object.keys(dataObj);
    const values = Object.values(dataObj);
    const total = values.reduce((a, b) => a + b, 0);

    const w = canvas.width = canvas.parentElement.clientWidth;
    const h = canvas.height = canvas.parentElement.clientHeight || 260;

    ctx.clearRect(0, 0, w, h);

    if (total === 0) {
        drawEmptyMessage(ctx, w, h);
        return;
    }

    const centerX = w / 3;
    const centerY = h / 2;
    const outerRadius = Math.min(centerX, centerY) - 20;
    const innerRadius = outerRadius * 0.55;

    const colors = ['#2563eb', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4', '#64748b'];

    let startAngle = -Math.PI / 2;

    labels.forEach((label, i) => {
        const sliceAngle = (values[i] / total) * 2 * Math.PI;

        ctx.fillStyle = colors[i % colors.length];
        ctx.beginPath();
        ctx.arc(centerX, centerY, outerRadius, startAngle, startAngle + sliceAngle);
        ctx.arc(centerX, centerY, innerRadius, startAngle + sliceAngle, startAngle, true);
        ctx.closePath();
        ctx.fill();

        startAngle += sliceAngle;
    });

    // Legend on the right side
    const legendX = (w / 3) * 2 - 20;
    let legendY = 40;

    labels.forEach((label, i) => {
        ctx.fillStyle = colors[i % colors.length];
        ctx.fillRect(legendX, legendY, 12, 12);

        ctx.fillStyle = '#0f172a';
        ctx.font = '500 12px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(`${label} (${values[i]})`, legendX + 20, legendY + 10);

        legendY += 24;
    });
}

function renderPieChart(canvas, dataObj) {
    const ctx = canvas.getContext('2d');
    const labels = Object.keys(dataObj);
    const values = Object.values(dataObj);
    const total = values.reduce((a, b) => a + b, 0);

    const w = canvas.width = canvas.parentElement.clientWidth;
    const h = canvas.height = canvas.parentElement.clientHeight || 260;

    ctx.clearRect(0, 0, w, h);

    if (total === 0) {
        drawEmptyMessage(ctx, w, h);
        return;
    }

    const centerX = w / 3;
    const centerY = h / 2;
    const radius = Math.min(centerX, centerY) - 20;

    const colors = ['#10b981', '#f59e0b', '#ef4444'];

    let startAngle = -Math.PI / 2;

    labels.forEach((label, i) => {
        const sliceAngle = (values[i] / total) * 2 * Math.PI;

        ctx.fillStyle = colors[i % colors.length];
        ctx.beginPath();
        ctx.moveTo(centerX, centerY);
        ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle);
        ctx.closePath();
        ctx.fill();

        startAngle += sliceAngle;
    });

    // Legend
    const legendX = (w / 3) * 2 - 20;
    let legendY = 60;

    labels.forEach((label, i) => {
        ctx.fillStyle = colors[i % colors.length];
        ctx.fillRect(legendX, legendY, 12, 12);

        ctx.fillStyle = '#0f172a';
        ctx.font = '500 12px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(`${label}: ${values[i]}`, legendX + 20, legendY + 10);

        legendY += 28;
    });
}

function drawEmptyMessage(ctx, w, h) {
    ctx.fillStyle = '#94a3b8';
    ctx.font = '500 13px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('No data available to plot', w / 2, h / 2);
}
