import React, { useState } from "react"
import { Button, Card, Col, Container, Form, Row } from "react-bootstrap";

export interface ConnectionInfo {
    serverAddress: string,
    sessionID: string
}

export default function ConnectionPrompt(props: { onConnect: (serverAddress: string, sessionID: string) => void }) {
    const [serverAddress, setServerAddress] = useState('');
    const [sessionID, setSessionID] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        props.onConnect(serverAddress, sessionID);
        console.log("connecting");
    }

    return (
        <Container fluid>
            <Card style={{ width: '40em' }}>
                <Card.Header>Connect to server</Card.Header>
                <Card.Body>
                    <Form onSubmit={handleSubmit}>
                        <Form.Group as={Row} className='mb-3'>
                            <Form.Label column sm={3}>Server Address</Form.Label>
                            <Col sm={9}><Form.Control type='text' placeholder="" value={serverAddress} onChange={e => {
                                setServerAddress(e.target.value)
                            }} />
                            </Col>
                        </Form.Group>
                        <Form.Group as={Row} className='mb-3'>
                            <Form.Label column sm={3}>Session ID</Form.Label>
                            <Col sm={9}><Form.Control type='text' placeholder="" value={sessionID} onChange={e => {
                                setSessionID(e.target.value)
                            }} />
                            </Col>
                        </Form.Group>
                        <Button variant='primary' type='submit'>Connect</Button>
                    </Form>
                </Card.Body>
            </Card>
        </Container>
    )
}