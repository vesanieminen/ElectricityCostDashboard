import {html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
//import '@vaadin/button'
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
    seriesTitle2?: string = '';

    @property()
    postfix: string = 'c/kWh';

    @property()
    averageText: string = '';

    @property()
    average?: number;

    @property()
    currentHour: number = 0;

    @property()
    predictionTimestamp: string = '' + Number.MAX_VALUE;

    @property()
    values?: Array<Nordpool>;

    @property()
    values2?: Array<Nordpool>;

    private getChartOptions(): Options {
        const extraDay = 24 * 60 * 60 * 1000; // One day in milliseconds
        const maxDisplayRange = extraDay * (12 / 24);
        const minTime = this.values ? this.values[this.values?.length - 1].time : 0;
        const maxTime = minTime + maxDisplayRange; // Set maxTime to 2 days after the start of the data
        return {
            navigator: {
                adaptToUpdatedData: true,
                enabled: true,
                series: [
                    {
                        data: this.values2!.map(item => ({
                            x: item.time,
                            y: item.price,
                        })),
                        type: "line", // Optional: Different type for second series
                    }
                ],
            },
            rangeSelector: {
                enabled: true,
                verticalAlign: 'top',
                x: 0,
                y: 0,
                selected: 0,
                buttons: [{
                    type: 'millisecond',
                    // @ts-ignore
                    count: this.values?.at(this.values?.length - 1).time - this.currentHour * 1000 + maxDisplayRange,
                    text: get("column-chart.now"),
                    //offsetMin: -86400000,
                    //offsetMax: -86400000,
                }, {
                    type: 'millisecond',
                    // @ts-ignore
                    count: this.values?.at(this.values?.length - 1).time - this.currentHour * 1000 + extraDay * 1.5 - extraDay * ((24 - new Date().getHours()) / 24),
                    text: get("column-chart.1d"),

                }, {
                    type: 'millisecond',
                    // @ts-ignore
                    count: this.values?.at(this.values?.length - 1).time - this.currentHour * 1000 + extraDay * 2.5 - extraDay * ((24 - new Date().getHours()) / 24),
                    text: get("column-chart.2d"),
                }, {
                    type: 'millisecond',
                    // @ts-ignore
                    count: this.values?.at(this.values?.length - 1).time - this.currentHour * 1000 + extraDay * 3.5 - extraDay * ((24 - new Date().getHours()) / 24),
                    text: get("column-chart.3d"),
                }, {
                    type: 'millisecond',
                    // @ts-ignore
                    count: this.values?.at(this.values?.length - 1).time - this.currentHour * 1000 + extraDay * 5.5 - extraDay * ((24 - new Date().getHours()) / 24),
                    text: get("column-chart.5d"),
                }
                    // , {
                    //     type: 'millisecond',
                    //     // @ts-ignore
                    //     count: this.values?.at(this.values?.length - 1).time - this.currentHour * 1000 + extraDay * 7.5,
                    //     text: get("column-chart.7d"),
                    // }
                ]
            },
            chart: {
                type: "column",
            },
            legend: {
                enabled: true
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
                min: minTime, // Set the minimum time for the chart
                max: maxTime, // Limit the maximum time for the chart to 2 days
                plotLines:
                    [{
                        value: this.currentHour * 1000,
                        className: "time"
                    }/*,{
                        value: Number(this.predictionTimestamp) + 3600000,
                        dashStyle: 'Dash', // Optional: Set dash style for the line
                        className: "prediction",
                        label: {
                            text: 'Prediction', // Label text
                            align: 'left',    // Align the text over the line
                            verticalAlign: 'top', // Align the text vertically
                            y: 10,              // Optional: Adjust the label position,
                            rotation: 0
                        },
                    }*/
                    ],
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
                    borderRadius: 5,
                    grouping: false,
                },
                series: {
                    states: {
                        hover: {
                            enabled: true, // Allow hover effects (e.g., label highlighting)
                        },
                        inactive: {
                            enabled: true, // Ensure inactive state is applied, but customize it
                            opacity: 1, // Prevent dimming by keeping opacity consistent
                        }
                    },
                    tooltip: {
                        valueSuffix: " " + this.postfix,
                        valueDecimals: 2,
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
            series: [
                {
                    name: this.seriesTitle,
                    type: "column",
                    //data: this.values!.concat(this.values2!).map(item => ({
                    data: this.values!.map(item => ({
                        x: item.time,
                        y: item.time > 1725138000000 ? item.price * 1.255 : item.price * 1.24,
                        className: item.time > Number(this.predictionTimestamp) ? "prediction" : "price"
                    })),
                    //pointPadding: 0.3,   // more space from neighbor series
                    //groupPadding: 0.05   // keep groups tight on the axis
                },
                {
                    name: this.seriesTitle2,
                    type: "column",
                    pointRange: 3600 * 1000, // 4 hours in ms, or 1 day etc
                    data: this.values2!.map(item => ({
                        x: item.time,
                        y: item.price,
                        className: "prediction"
                    })),
                    //pointWidth: 16,
                    //pointPadding: 0.3,   // more space from neighbor series
                    //groupPadding: 0.05   // keep groups tight on the axis
                }
            ],
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

