import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import {registerTranslateConfig, translate, use} from "lit-translate";

registerTranslateConfig({
    loader: lang => fetch(`${lang}.json`).then(res => res.json())
});

@customElement('help-view')
export class HelpView extends LitElement {

    static styles = css`
    `;

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
        this.classList.add('flex', 'flex-col', 'm-auto', 'px-m', 'max-w-screen-lg');

        await use(this.language);
        this.hasLoadedStrings = true;
    }

    createRenderRoot() {
        return this;
    }

    render() {
        return html`
            <div class="flex flex-col gap-s">
                <h2>${translate("help.title")}</h2>

                <h3>${translate("help.liukuri-live-pks-title")}</h3>
                <div class="flex-col mt-m">
                    <iframe style="aspect-ratio: 16 / 9; width: 100%;"
                            src="https://www.youtube.com/embed/MmN86VN0vYY?si=t_lB6lMkg17budxb"
                            title="YouTube video player" frameborder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                            allowfullscreen>
                    </iframe>

                </div>
                <p>${translate("help.liukuri-live-pks-description")}</p>

                <br/>

                <h3>${translate("help.liukuri-how-to-1-title")}</h3>
                <div class="flex-col mt-m">
                    <iframe style="aspect-ratio: 16 / 9; width: 100%;"
                            src="https://www.youtube.com/embed/AYdYcQeVIGE?si=mq1ekAUHRTteUVWP"
                            title="YouTube video player" frameborder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                            allowfullscreen>
                    </iframe>

                </div>
                <p>${translate("help.liukuri-how-to-1-description")}</p>

                <br/>

                <h3>${translate("help.liukuri-presentation-title")}</h3>
                <div class="flex-col mt-m">
                    <iframe style="aspect-ratio: 16 / 9; width: 100%;"
                            src="https://www.youtube.com/embed/Y4Uo0EcBMMI?si=87BnZEjPTaOyd64R"
                            title="YouTube video player" frameborder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                            allowfullscreen>
                    </iframe>

                </div>
                <p>${translate("help.liukuri-presentation-description")}</p>
                <p>${translate("help.liukuri-presentation-description-2")}</p>
            </div>
        `;
    }

}
