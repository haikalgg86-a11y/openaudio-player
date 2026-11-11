cmake_minimum_required(VERSION 3.22.1)
project("openaudio_decoder" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(openaudio_decoder SHARED
    lossless_pcm_decoder.cpp
    integer_pcm_hasher.cpp
)

# Mandatory Android 16 (API 36) 16 KB ELF Page Size Alignment
target_link_options(openaudio_decoder PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)

find_library(log-lib log)
target_link_libraries(openaudio_decoder
    ${log-lib}
)
