import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import {registerTranslateConfig, translate, translateUnsafeHTML, use} from "lit-translate";

registerTranslateConfig({
    loader: lang => fetch(`${lang}.json`).then(res => res.json())
});

@customElement('about-view')
export class AboutView extends LitElement {

    static styles = css`
  `;

    @property()
    language: string = 'en';

    @property()
    versioninfo: string = 'n/a';

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
                <h2>${translate("about.liukuri.title")}</h2>
                <div>${translateUnsafeHTML("about.liukuri.descriptionHTML")}</div>

                <h2>${translate("about.design.title")}</h2>
                <div>${translate("about.design.description")}</div>

                <h2>${translate("about.logo.title")}</h2>
                <div>${translateUnsafeHTML("about.logo.descriptionHTML")}</div>
            </div>
            <div class="m-auto">
                <img class="w-full h-full" src="icons/Liukurikettu1c.png" alt="logo"/>
            </div>
            <p>${translate("about.version")}: ${this.versioninfo}</p>
        `;
    }

}
