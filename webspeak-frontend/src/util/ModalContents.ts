export enum ModalType {
    STANDARD,
    WARNING,
    ERROR
}

export default interface ModalContents {
    type: ModalType,
    title: string,
    detail?: string
}