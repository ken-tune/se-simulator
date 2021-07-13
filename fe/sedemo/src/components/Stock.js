import { Row, Col, Button, Dropdown, Table, DropdownButton, Container } from 'react-bootstrap';
import { useEffect, useState } from 'react';
import DropdownItem from 'react-bootstrap/esm/DropdownItem';

function Stock(props) {
    const [tickerAggVolume, setTickerAggVolume] = useState("");
    const [tickerHighPrice, setTickerHighPrice] = useState("");
    const [tickers, setTickers] = useState(["A.COM", "B.COM", "C.COM"])
    const [tickerValue, setTickerValue] = useState("Choose Stock");
    const [tickerVolumeForPrice, setTickerVolumeForPrice]  = useState(0);
    const [tickerPrice, setTickerPrice] = useState(0);
    const INTERVAL = 2000;

    const timer = (e) => {
        setInterval(() => {
            getAggregateVolume(e);
        }, INTERVAL);
    }

    const highPriceTimer = (e) => {
        setInterval(() => {
            getHighestPrice(e);
        }, INTERVAL);
    }

    function getAggregateVolume(e) {
        console.log("Ticker Value ::", tickerValue);
        const headers = {
            'accept': 'application/json',
        }
        fetch('/tickerAggregatePrice/' + tickerValue, {
            method: 'get',
            headers: headers
        })
            .then((response) => response.json())
            .then((result) => {
                console.log(result);
                setTickerAggVolume(result);
            },
                (error) => {
                    console.log("Error in Fetching data", error);
                }
            )
    }
    function getHighestPrice(e) {
        console.log("Ticker Value ::", tickerValue);
        const headers = {
            'accept': 'application/json',
        }
        fetch('/tickerHighestPrice/' + tickerValue, {
            method: 'get',
            headers: headers
        })
            .then((response) => response.json())
            .then((result) => {
                console.log("Highest Price", result);
                setTickerHighPrice(result);
            },
                (error) => {
                    console.log("Error in Fetching data", error);
                }
            )
    }
    
    function updateTickerValue(e) {
        setTickerValue(e);
    }

    function setTickerVolume(e) {
        setTickerVolumeForPrice(e.target.value);
    }
    return (
        <Container class="align-middle">
            <p></p>
            <Row>
                <Col sm="8">
                    <h2> Aerospike Stock Exchange Demo</h2>
            <br></br>
                    <DropdownButton title={tickerValue} value={tickerValue} onSelect = {updateTickerValue}>
                        {tickers.map(value => (
                            <Dropdown.Item eventKey = {value}> {value} </Dropdown.Item>
                        ))}
                    </DropdownButton>
                </Col>
            </Row>
            <p></p>
            <Row>
                <Col sm="8">
                    <Table striped bordered hover>
                        <thead>
                            <tr>
                                <th>Action</th>
                                <th>Value</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>
                                    <Button onClick={timer}>Aggregate Volume</Button>
                                </td>
                                <td>
                                    <p >{tickerAggVolume}</p>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <Button onClick={highPriceTimer}>Highest Price</Button>
                                </td>
                                <td>
                                    <p >{tickerHighPrice}</p>
                                </td>
                            </tr>
                        </tbody>
                    </Table>
                </Col>
            </Row>
        </Container>
    );

}
export default Stock;