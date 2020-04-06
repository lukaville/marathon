import React, {Component} from 'react';
import ReactLoading from 'react-loading';
import paths from "../utils/paths";

export default class ColorizedLogsComponent extends Component {
    state = {
        data: window.logs,
        logs: null
    };

    componentWillMount() {
        this.loadData(window.logs.log_path, function (text) {
            this.onDataReceived(text);
        }.bind(this));
    }

    onDataReceived(data) {
        let logs = _parseLogcatMessages(data);
        this.setState({logs: logs})
    }

    loadData(file, callback) {
        const rawFile = new XMLHttpRequest();
        rawFile.overrideMimeType("application/json");
        rawFile.open("GET", file, true);
        rawFile.onreadystatechange = function () {
            if (rawFile.readyState === 4) {
                if (rawFile.status === 200) {
                    callback(rawFile.responseText);
                }
                else {
                    callback("[]");
                }
            }
        };
        rawFile.send(null);
    }

    render() {
        return (
            <div className="content margin-top-20">
                <div className="title-common vertical-aligned-content">
                    <a href={paths.fromLogsToIndex}>Pools list</a> /
                    <a href={paths.fromLogsToPool(this.state.data.pool_id)}>Pool {this.state.data.pool_id}</a> /
                    <a href={paths.fromLogsToTest(this.state.data.test_id)}>{this.state.data.display_name}</a> /
                    Logs
                </div>


                <div className="card">
                    <table className="table logcat">
                        <tbody>
                        <tr>
                            <th>Process</th>
                            <th>Tag</th>
                            <th>Level</th>
                            <th>Time</th>
                            <th className="message">Message</th>
                        </tr>
                        {!!this.state.logs && this.state.logs
                            .map((message, index) =>
                                     (<tr className={_selectStyle(message.priority)} key={index}>
                                         <td>{message.processId}</td>
                                         <td>{message.tag}</td>
                                         <td>{message.priority}</td>
                                         <td className="formatted-time">{message.time}</td>
                                         <td>
                                             <pre>{message.message}</pre>
                                         </td>
                                     </tr>)
                            )}
                        </tbody>
                    </table>
                    {this.state.logs == null && <ReactLoading className="center"
                                                              type="bubbles"
                                                              color="#ff0000"
                                                              delay={1}/>}
                </div>
            </div>
        );
    }
}

function _selectStyle(logLevel) {
    switch (logLevel) {
        case "W":
            return "line warn";
        case "D": {
            return "line debug";
        }
        case "E": {
            return "line error";
        }
        case "I": {
            return "line info";
        }
        case "A": {
            return "line assert";
        }
        case "V": {
            return "line verbose";
        }
    }
}

/**
 * Pattern for header (MM-DD HH:MM:SS.mmm PID-TID/AppName LEVEL/TAG: message). Example:
 *
 * <pre>04-06 16:21:43.746 1852-25383/? D/GraphicBufferSource: got buffer with new dataSpace #104</pre>
 *
 * Group 1: Date + Time
 * Group 2: PID
 * Group 3: TID (hex on some systems!)
 * Group 4: Log Level character
 * Group 5: Tag
 */
function _parseLogcatMessages(response) {
    const dateTimeRegexp = "\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\.\\d\\d\\d";
    const processIdRegexp = "\\d+";
    const threadIdRegexp = "\\w+";
    const appNameRegexp = ".*?";
    const priorityRegexp = "[VDIWEAF]";
    const tagRegexp = ".*?";
    const messageRegexp = ".*";
    const headerRegexp = new RegExp(
        `^(${dateTimeRegexp}) (${processIdRegexp})-(${threadIdRegexp})/(${appNameRegexp}) (${priorityRegexp})/(${tagRegexp}): (${messageRegexp})`
    );

    const result = [];
    let lastMessage = null;

    response
        .split("\n")
        .filter(v => v !== '')
        .forEach((line) => {
            const headerParingResult = headerRegexp.exec(line);

            if (headerParingResult) {
                const time = headerParingResult[1];
                const processId = headerParingResult[2];
                const threadId = headerParingResult[3];
                const appName = headerParingResult[4];
                const priority = headerParingResult[5];
                const tag = headerParingResult[6];
                const message = headerParingResult[7];

                if (lastMessage != null) {
                    result.push(lastMessage);
                }
                lastMessage = {
                    time,
                    processId,
                    threadId,
                    appName,
                    priority,
                    tag,
                    message
                }
            }
            else {
                if (lastMessage != null) {
                    lastMessage.message = lastMessage.message + '\n' + line
                }
            }
        });

    if (lastMessage != null) {
        result.push(lastMessage);
    }

    return result
}
