import {html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';
import type {Options} from 'highcharts';
import {get, registerTranslateConfig, use} from "lit-translate";
import {Nordpool} from "Frontend/src/nordpool";

registerTranslateConfig({
    loader: lang => fetch(`${lang}.json`).then(res => res.json())
});

@customElement('history-template')
export class HistoryTemplate extends LitElement {

    @property()
    language: string = 'en';

    // Defer the first update of the component until the strings has been loaded to avoid empty strings being shown
    @state() hasLoadedStrings = false;

    protected shouldUpdate(props: PropertyValues) {
        return this.hasLoadedStrings && super.shouldUpdate(props);
    }

    // Load the initial language and mark that the strings has been loaded so the component can render.
    async connectedCallback() {
        super.connectedCallback();
        await use(this.language);
        this.hasLoadedStrings = true;
    }

    @property()
    seriesTitle?: string = '';

    @property()
    postfix: string = 'c/kWh';

    @property()
    currentHour: number = 0;

    @property()
    dayOfMonth: number = 1;

    @property()
    values?: Array<Nordpool>;

    private getChartOptions(): Options {
        return {
            scrollbar: {
                enabled: true,
                liveRedraw: true,
            },
            rangeSelector: {
                enabled: true,
                verticalAlign: 'top',
                x: 0,
                y: 0,
                selected: 2,
                buttons: [
                    {
                        type: 'day',
                        count: 7,
                        text: get("column-chart.7d")
                    }, {
                        type: 'day',
                        count: 14,
                        text: get("column-chart.14d")
                    }, {
                        type: 'day',
                        count: this.dayOfMonth,
                        text: get("column-chart.mtd")
                    }, {
                        type: 'month',
                        count: 1,
                        text: get("column-chart.1m")
                    }, {
                        type: 'month',
                        count: 3,
                        text: get("column-chart.3m")
                    }, {
                        type: 'month',
                        count: 6,
                        text: get("column-chart.6m")
                    }, {
                        type: 'ytd',
                        text: get("column-chart.ytd")
                    }, {
                        type: 'month',
                        count: 12,
                        text: get("column-chart.12m")
                    }
                ]
            },
            boost: {
                useGPUTranslations: true
            },
            navigator: {
                enabled: true
            },
            legend: {
                enabled: false
            },
            tooltip: {
                shared: true,
                xDateFormat: "%A<br />%H:%M %e.%m.%Y",
            },
            time: {
                useUTC: false,
                timezone: 'Europe/Helsinki'
            },
            xAxis: {
                type: "datetime",
                crosshair: true,
                plotLines:
                    [{
                        value: this.currentHour * 1000,
                        className: "time"
                    }],
            },
            yAxis: [{
                softMin: 0,
                title: {
                    text: ''
                    //text: get("general.price-type")
                }
            }],
            plotOptions: {
                column: {
                    pointPadding: 0,
                    groupPadding: 0,
                    borderWidth: 0,
                },
                series: {
                    tooltip: {
                        valueSuffix: " " + this.postfix,
                        valueDecimals: 2
                    },
                    animation: false,
                    // Works but is buggy when switching back and forth from different date ranges.
                    /*marker: {
                        enabled: false
                    },*/
                },
            },
            series: [{
                name: this.seriesTitle,
                type: "line",
                data: this.values!.map(item => [item.time, item.price]),
                boostThreshold: 100
            }],
        };
    }

    createRenderRoot() {
        // Do not use a shadow root
        return this;
    }

    render() {
        return html`
            <vaadin-chart
                    style="height: var(--fullscreen-height-history); min-height: 300px"
                    .additionalOptions=${this.getChartOptions()}
            >
            </vaadin-chart>
        `;
    }

}


