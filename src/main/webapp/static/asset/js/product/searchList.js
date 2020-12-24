import {checkCart, setGoButtons} from '../component/searchHeader.js';
import {makeCommas, deleteCommas} from "../component/price_quantity.js";

const prices = document.querySelectorAll(".js-price");

function init(){
    checkCart();
    setGoButtons();
    [...prices].forEach(el => el.innerText = makeCommas(el.innerText));
}
init();