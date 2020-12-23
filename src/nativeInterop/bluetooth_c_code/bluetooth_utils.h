#include <bluetooth/bluetooth.h>
#include <bluetooth/sdp.h>
#include <termios.h>
#include <bluetooth/sdp_lib.h>

sdp_session_t* register_rfcomm_sdp(uint8_t rfcomm_channel, char* service_name, char* service_dsc, char* service_prov);
void setupSerial(int fd);
