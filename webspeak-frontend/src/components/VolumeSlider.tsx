import { Form } from "react-bootstrap"

interface SliderProps {
    value: number,
    min?: number,
    max?: number,
    onChange?: (value: number) => void
}

export default function VolumeSlider({ value, min=0, max=1, onChange }: SliderProps) {
    return <Form.Range step={.01} value={value} min={min} max={max} onChange={e => {
        if (onChange) onChange(parseFloat(e.target.value))
    }} />
}