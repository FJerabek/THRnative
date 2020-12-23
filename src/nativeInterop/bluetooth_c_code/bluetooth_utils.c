#include "bluetooth_utils.h"
sdp_session_t* register_rfcomm_sdp(uint8_t rfcomm_channel, char* service_name, char* service_dsc, char* service_prov) {
    uint32_t service_uuid_int[] = { 0x01110000, 0x00100000, 0x80000080, 0xfb349b5f};

    uuid_t root_uuid, l2cap_uuid, rfcomm_uuid, svc_uuid;
    sdp_list_t *l2cap_list,
            *rfcomm_list,
            *root_list,
            *proto_list,
            *profile_desc_list,
            *service_classes,
            *access_proto_list;
    sdp_data_t *channel;

    sdp_record_t *record = sdp_record_alloc();

    // set the general service ID
    sdp_uuid128_create( &svc_uuid, &service_uuid_int );
    sdp_set_service_id( record, svc_uuid );

    // make the service record publicly browsable
    sdp_uuid16_create(&root_uuid, PUBLIC_BROWSE_GROUP);
    root_list = sdp_list_append(0, &root_uuid);
    sdp_set_browse_groups( record, root_list );

    // set l2cap information
    sdp_uuid16_create(&l2cap_uuid, L2CAP_UUID);
    l2cap_list = sdp_list_append( 0, &l2cap_uuid );
    proto_list = sdp_list_append( 0, l2cap_list );

    // set rfcomm information
    sdp_uuid16_create(&rfcomm_uuid, RFCOMM_UUID);
    channel = sdp_data_alloc(SDP_UINT8, &rfcomm_channel);
    rfcomm_list = sdp_list_append( 0, &rfcomm_uuid );
    sdp_list_append( rfcomm_list, channel );
    sdp_list_append( proto_list, rfcomm_list );

    // attach protocol information to service record
    access_proto_list = sdp_list_append( 0, proto_list );
    sdp_set_access_protos( record, access_proto_list );


    sdp_profile_desc_t profileDesc;

    sdp_uuid16_create(&profileDesc.uuid, SERIAL_PORT_PROFILE_ID);
    profileDesc.version = 0x100;

    profile_desc_list = sdp_list_append(0, &profileDesc);

    sdp_set_profile_descs(record, profile_desc_list);

    service_classes = sdp_list_append(0, &svc_uuid);

    sdp_set_service_classes(record, service_classes);


    // set the name, provider, and description
    sdp_set_info_attr(record, service_name, service_prov, service_dsc);

    sdp_session_t *session;

    // connect to the local SDP server, register the service record, and
    // disconnect
    session = sdp_connect( BDADDR_ANY, BDADDR_LOCAL, SDP_RETRY_IF_BUSY );
    sdp_record_register(session, record, 0);

    // cleanup
    sdp_data_free( channel );
    sdp_list_free( l2cap_list, 0 );
    sdp_list_free( rfcomm_list, 0 );
    sdp_list_free( root_list, 0 );
    sdp_list_free( access_proto_list, 0 );

    return session;
}

void setupSerial(int fd) {
    struct termios options;
    tcgetattr(fd, &options);
    options.c_cflag = B115200 | CS8 | CLOCAL | CREAD;
    options.c_iflag = IGNPAR;
    options.c_oflag = 0;
    options.c_lflag = 0;
    tcflush(fd, TCIFLUSH);
    tcsetattr(fd, TCSANOW, &options);
}
