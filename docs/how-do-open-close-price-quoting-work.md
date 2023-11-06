### Open Prices
- If an open-price was entered by the user, then that open-price will be used.
- If there is no open-price, and no open-date was selected, then the latest intraday price will be used.
- If there is no open-price, but an open-date was selected, then:
    - If the open-date is historic, then the price from that day will be used.
    - If the open-date is for today, then the latest intraday price will be used.

### Close Prices
- If a close-price was entered by the user, then that close-price will be used.
- If there is no close-price, and no close-date was selected, then the latest intraday price will be used.
- If there is no close-price, but a close-date was selected, then:
    - If the close-date is historic, then the price from that day will be used.
    - If the close-date is for today, then the latest intraday price will be used.