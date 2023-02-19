import {html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import '@vaadin/vaadin-button'
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';
import type {Options} from 'highcharts';
import {registerTranslateConfig, use} from "lit-translate";

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
    values?: Array<Nordpool>;

    private getChartOptions(): Options {
        return {
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
                        text: "7d"
                    }, {
                        type: 'day',
                        count: 14,
                        text: "14d"
                    }, {
                        type: 'month',
                        count: 1,
                        text: "1m"
                    }, {
                        type: 'month',
                        count: 3,
                        text: "3m"
                    }, {
                        type: 'month',
                        count: 6,
                        text: "6m"
                    }, {
                        type: 'ytd',
                        text: "YTD"
                    }, {
                        type: 'month',
                        count: 12,
                        text: "12m"
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
                    style="height: 100%"
                    .additionalOptions=${this.getChartOptions()}
            >
            </vaadin-chart>
        `;
    }

}

interface Nordpool {
    time: number;
    price: number;
}


