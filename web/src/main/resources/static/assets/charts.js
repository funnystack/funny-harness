/**
 * 公共图表模块
 * ECharts 折线图/饼图/热力图封装
 */

// ========== 折线图 ==========
function renderLineChart(
  containerId,
  title,
  data,
  xField,
  yField,
  options = {},
) {
  const chart = echarts.init(document.getElementById(containerId));
  const xData = data.map((d) => d[xField]);
  const yData = data.map((d) => d[yField]);

  chart.setOption({
    title: { text: title, left: "center", textStyle: { fontSize: 14 } },
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: xData },
    yAxis: { type: "value", name: options.yName || "" },
    series: [
      {
        type: "line",
        data: yData,
        smooth: true,
        areaStyle: { opacity: 0.3 },
        itemStyle: { color: options.color || "#409eff" },
      },
    ],
    grid: { left: "10%", right: "5%", bottom: "15%" },
  });

  window.addEventListener("resize", () => chart.resize());
  return chart;
}

// ========== 多折线图 ==========
function renderMultiLineChart(containerId, title, data, xField, seriesConfig) {
  const chart = echarts.init(document.getElementById(containerId));
  const xData = [...new Set(data.map((d) => d[xField]))];

  const series = seriesConfig.map((config) => ({
    name: config.name,
    type: "line",
    data: xData.map((x) => {
      const item = data.find(
        (d) => d[xField] === x && d[config.groupField] === config.groupValue,
      );
      return item ? item[config.yField] : 0;
    }),
    smooth: true,
  }));

  chart.setOption({
    title: { text: title, left: "center", textStyle: { fontSize: 14 } },
    tooltip: { trigger: "axis" },
    legend: { data: seriesConfig.map((s) => s.name), bottom: 0 },
    xAxis: { type: "category", data: xData },
    yAxis: { type: "value" },
    series,
    grid: { left: "10%", right: "5%", bottom: "15%" },
  });

  window.addEventListener("resize", () => chart.resize());
  return chart;
}

// ========== 饼图 ==========
function renderPieChart(containerId, title, data, nameField, valueField) {
  const chart = echarts.init(document.getElementById(containerId));

  chart.setOption({
    title: { text: title, left: "center", textStyle: { fontSize: 14 } },
    tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
    series: [
      {
        type: "pie",
        radius: ["40%", "70%"],
        data: data.map((d) => ({ name: d[nameField], value: d[valueField] })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: "rgba(0,0,0,0.5)",
          },
        },
      },
    ],
  });

  window.addEventListener("resize", () => chart.resize());
  return chart;
}

// ========== 柱状图 ==========
function renderBarChart(
  containerId,
  title,
  data,
  xField,
  yField,
  options = {},
) {
  const chart = echarts.init(document.getElementById(containerId));

  chart.setOption({
    title: { text: title, left: "center", textStyle: { fontSize: 14 } },
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: data.map((d) => d[xField]) },
    yAxis: { type: "value", name: options.yName || "" },
    series: [
      {
        type: "bar",
        data: data.map((d) => d[yField]),
        itemStyle: { color: options.color || "#409eff" },
      },
    ],
    grid: { left: "10%", right: "5%", bottom: "15%" },
  });

  window.addEventListener("resize", () => chart.resize());
  return chart;
}

// ========== 热力图 ==========
function renderHeatmapChart(
  containerId,
  title,
  data,
  xField,
  yField,
  valueField,
) {
  const chart = echarts.init(document.getElementById(containerId));
  const xData = [...new Set(data.map((d) => d[xField]))];
  const yData = [...new Set(data.map((d) => d[yField]))];
  const heatData = data.map((d) => [
    xData.indexOf(d[xField]),
    yData.indexOf(d[yField]),
    d[valueField],
  ]);

  chart.setOption({
    title: { text: title, left: "center", textStyle: { fontSize: 14 } },
    tooltip: { position: "top" },
    xAxis: { type: "category", data: xData, splitArea: { show: true } },
    yAxis: { type: "category", data: yData, splitArea: { show: true } },
    visualMap: {
      min: 0,
      max: Math.max(...data.map((d) => d[valueField]), 1),
      calculable: true,
      orient: "horizontal",
      left: "center",
      bottom: 0,
    },
    series: [
      {
        type: "heatmap",
        data: heatData,
        label: { show: true },
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowColor: "rgba(0,0,0,0.5)" },
        },
      },
    ],
  });

  window.addEventListener("resize", () => chart.resize());
  return chart;
}
