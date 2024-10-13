import {
    Box,
    ChakraProvider,
    IconButton,
    Image,
    Slider,
    SliderFilledTrack,
    SliderThumb,
    SliderTrack
} from "@chakra-ui/react";

const MainUI = () => (
  <ChakraProvider resetCSS>
    <Box
      display="flex"
      flexDirection="column"
      justifyContent="center"
      alignItems="center"
      width="100vw"
      height="100vh"
      backgroundColor="orange.500"
    >
      <Box
        width="200px"
        height="400px"
        backgroundColor="blackAlpha.500"
        borderRadius="10px"
        display="flex"
        flexDirection="column"
        justifyContent="flex-end"
        alignItems="center"
      >
        <Box
          display="flex"
          justifyContent="center"
          border="8%"
          borderRadius="50%"
          width="60%"
          mb="42%"
          opacity={1}
          backgroundColor="whiteAlpha.500"
          aspectRatio="1/1"
          alignItems="center"
        >
          <IconButton
            aria-label="icon"
            icon={
              <Image
                height="50%"
                width="50%"
                //this is temporary. Use something stored locally.
                src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1em' height='1em' viewBox='0 0 24 24'%3E%3Cpath fill='%23000' d='M12 14q-1.25 0-2.125-.875T9 11V5q0-1.25.875-2.125T12 2t2.125.875T15 5v6q0 1.25-.875 2.125T12 14m-1 7v-3.075q-2.6-.35-4.3-2.325T5 11h2q0 2.075 1.463 3.538T12 16t3.538-1.463T17 11h2q0 2.625-1.7 4.6T13 17.925V21z'/%3E%3C/svg%3E"
              />
            }
            size="md"
            variant="ghost"
            height="100%"
            width="100%"
            borderRadius="50%"
          />
        </Box>
        <Slider
          aria-label="input volume"
          defaultValue={50}
          mb="10%"
          width="90%"
          aspectRatio="6.66/1"
        >
          <SliderTrack
            boxSize={"100%"}
            bg="red.100"
            borderRadius="10px"
            backgroundColor="pink.500"
          >
            <SliderFilledTrack bg="tomato" />
          </SliderTrack>
          <SliderThumb
            height="99%"
            width="15%"
            //add a tooltip here to display current exact volume
            backgroundColor="green.200"
          >
            <Image
              height="80%"
              width="80%"
              //this is temporary. Use something stored locally.
              src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1em' height='1em' viewBox='0 0 24 24'%3E%3Cpath fill='%23000' d='M12 14q-1.25 0-2.125-.875T9 11V5q0-1.25.875-2.125T12 2t2.125.875T15 5v6q0 1.25-.875 2.125T12 14m-1 7v-3.075q-2.6-.35-4.3-2.325T5 11h2q0 2.075 1.463 3.538T12 16t3.538-1.463T17 11h2q0 2.625-1.7 4.6T13 17.925V21z'/%3E%3C/svg%3E"
            />
          </SliderThumb>
        </Slider>
        <Slider
          aria-label="output volume"
          defaultValue={50}
          mb="24%"
          width="90%"
          aspectRatio="6.66/1"
        >
          <SliderTrack
            boxSize={"100%"}
            bg="red.100"
            borderRadius="10px"
            backgroundColor="pink.500"
          >
            <SliderFilledTrack bg="tomato" />
          </SliderTrack>
          <SliderThumb height="99%" width="15%" backgroundColor="green.200">
            <Image
              height="80%"
              width="80%"
              //this is temporary. Use something stored locally.
              src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1em' height='1em' viewBox='0 0 24 24'%3E%3Cpath fill='%23000' d='M2 16h3.889l5.294 4.332a.5.5 0 0 0 .817-.387V4.055a.5.5 0 0 0-.817-.387L5.89 8H2a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1m21-4c0 3.292-1.446 6.246-3.738 8.262l-1.418-1.418A8.98 8.98 0 0 0 21 12a8.98 8.98 0 0 0-3.155-6.844l1.417-1.418A10.97 10.97 0 0 1 23 12m-5 0a5.99 5.99 0 0 0-2.287-4.713l-1.429 1.429A4 4 0 0 1 16 12c0 1.36-.679 2.561-1.716 3.284l1.43 1.43A5.99 5.99 0 0 0 18 12'/%3E%3C/svg%3E"
            />
          </SliderThumb>
        </Slider>
      </Box>
    </Box>
  </ChakraProvider>
);

export default MainUI;