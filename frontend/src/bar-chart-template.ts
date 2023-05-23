import {html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import '@vaadin/vaadin-button'
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';
import type {Options} from 'highcharts';
import {get, registerTranslateConfig, use} from "lit-translate";
import {Nordpool} from "Frontend/src/nordpool";

registerTranslateConfig({
    loader: lang => fetch(`${lang}.json`).then(res => res.json())
});

@customElement('bar-chart-template')
export class BarChartTemplate extends LitElement {

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
    chartTitle?: string = '';

    @property()
    seriesTitle?: string = '';

    @property()
    postfix: string = 'c/kWh';

    @property()
    averageText: string = '';

    @property()
    average?: number;

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
                selected: 0,
                buttons: [{
                    type: 'millisecond',
                    // @ts-ignore
                    count: this.values?.at(this.values?.length - 1).time - this.currentHour * 1000,
                    text: get("column-chart.now"),
                    //offsetMin: -86400000,
                    //offsetMax: -86400000,
                }, {
                    type: 'day',
                    count: 1,
                    text: get("column-chart.1d"),

                }, {
                    type: 'day',
                    count: 2,
                    text: get("column-chart.2d"),
                }, {
                    type: 'day',
                    count: 3,
                    text: get("column-chart.3d"),
                }, {
                    type: 'day',
                    count: 5,
                    text: get("column-chart.5d"),
                }, {
                    type: 'all',
                    text: get("column-chart.7d"),
                }]
            },
            chart: {
                type: "column"
            },
            legend: {
                enabled: false
            },
            tooltip: {
                shared: true,
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
                title: {
                    text: ''
                    //text: get("general.price-type")
                },
                softMax: this.average! + 1,
                plotLines:
                    [{
                        //label: {
                        //    text: this.averageText
                        //},
                        value: this.average
                    }, {
                        className: "average-per-2",
                        value: this.average! / 2
                    }],
            }],
            plotOptions: {
                column: {
                    borderRadius: 5
                },
                series: {
                    tooltip: {
                        valueSuffix: " " + this.postfix,
                        valueDecimals: 2
                    },
                    zones: [{
                        value: -10,
                        className: "zone-0"
                    }, {
                        value: this.average! / 2,
                        className: "zone-1"
                    }, {
                        value: this.average,
                        className: "zone-2"
                    }, {
                        className: "zone-3"
                    }]
                    ,
                    animation: false
                },
            },
            series: [{
                name: this.seriesTitle,
                type: "column",
                data: this.values!.map(item => [item.time, item.price > 0 ? item.price * 1.24 : item.price])
            }],
        };
    }

    createRenderRoot() {
        // Do not use a shadow root
        return this;
    }

    previous() {
        this.$server!.previous();
    }

    next() {
        this.$server!.next();
    }

    private $server?: ChartTemplateServerInterface;

    render() {
        return html`
            <!--div class="flex justify-center items-center">
                <vaadin-button class="h-s" theme="tertiary" @click=${this.previous}>
                    <span class="material-icons w-l">chevron_left</span>
                </vaadin-button>
                <h2 class="m-s text-l">${this.chartTitle}</h2>
                <vaadin-button class="h-s" theme="tertiary" @click=${this.next}>
                    <span class="material-icons w-l">chevron_right</span>
                </vaadin-button>
            </div-->
            <vaadin-chart
                    theme="column"
                    style="height: 100%"
                    .additionalOptions=${this.getChartOptions()}
            >
            </vaadin-chart>
        `;
    }

}

interface ChartTemplateServerInterface {
    previous(): void;
    next(): void;
}

