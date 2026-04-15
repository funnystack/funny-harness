/**
 * 公共 JS 模块
 * Axios 封装 + 侧边栏导航布局
 */

// ========== Axios 封装 ==========
const http = axios.create({
  baseURL: "/api/metrics",
  timeout: 10000,
});

// ========== 侧边栏导航 ==========
function renderSidebar(activePage) {
  const navItems = [
    { href: "/dashboard/overview.html", label: "度量总览", id: "overview" },
    {
      href: "/dashboard/framework.html",
      label: "三层指标体系",
      id: "framework",
    },
    {
      href: "/dashboard/architecture.html",
      label: "整体架构",
      id: "architecture",
    },
    { href: "/dashboard/daily.html", label: "日报看板", id: "daily" },
    { href: "/dashboard/weekly.html", label: "周报看板", id: "weekly" },
    { href: "/dashboard/monthly.html", label: "月报看板", id: "monthly" },
    {
      href: "/dashboard/agent/dashboard.html",
      label: "Agent 运营",
      id: "agent",
    },
    { href: "/dashboard/alerts.html", label: "告警历史", id: "alerts" },
  ];

  return `
    <div style="width:200px;min-height:100vh;background:#304156;color:#fff;position:fixed;left:0;top:0;padding-top:20px;">
      <div style="padding:0 20px 20px;font-size:18px;font-weight:bold;border-bottom:1px solid #3c4f65;">Metrics Dashboard</div>
      <div style="padding-top:10px;">
        ${navItems
          .map(
            (item) => `
          <a href="${item.href}" style="display:block;padding:12px 20px;color:#bfcbd9;text-decoration:none;
            ${activePage === item.id ? "background:#263445;color:#409eff;" : ""}
          " onmouseover="this.style.background='#263445'" onmouseout="this.style.background='${
            activePage === item.id ? "#263445" : "transparent"
          }'">
            ${item.label}
          </a>
        `,
          )
          .join("")}
      </div>
    </div>
  `;
}

// ========== 页面骨架 ==========
function renderPage(title, activePage, contentHtml) {
  document.body.innerHTML = `
    ${renderSidebar(activePage)}
    <div style="margin-left:200px;padding:20px 30px;">
      <h2 style="margin:0 0 20px;color:#303133;">${title}</h2>
      ${contentHtml}
    </div>
  `;
}

// ========== 工具函数 ==========
function formatNumber(num) {
  if (num === null || num === undefined) return "-";
  return Number(num).toLocaleString();
}

function formatDate(dateStr) {
  if (!dateStr) return "-";
  return dateStr.substring(0, 10);
}
