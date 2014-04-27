/** @jsx React.DOM */

var statusMap;

statusMap = {
  init: {
    icon: "time",
    detail: function() {
      return "Iniciando a busca...";
    }
  },
  info: {
    icon: "time",
    detail: function() {
      return "Carregando informações do vídeo...";
    }
  },
  upload: {
    icon: "upload",
    detail: function() {
      return "Enviando {this.statusInfo.upload} ...";
    }
  },
  search: {
    icon: "search",
    detail: function() {
      return "Buscando legendas no servidor: {this.statusInfo.search}";
    }
  },
  download: {
    icon: "download",
    detail: function() {
      return "Baixando legendas do servidor: {this.statusInfo.download.source.name()}...";
    }
  },
  downloaded: {
    icon: "check",
    detail: function() {
      return "Baixado do servidor \n{this.statusInfo.download.source.name()}\n{this.statusInfo.download.source.website()}";
    }
  },
  notfound: {
    icon: "error",
    detail: function() {
      return "Nenhuma legenda encontrada, tente novamente mais tarde";
    }
  },
  unchanged: {
    icon: "check-small",
    detail: function() {
      return "Você já tem a legenda no seu indioma favorito";
    }
  },
  uploaded: {
    icon: "check",
    detail: function() {
      return "Suas legendas locais para esse vídeo foram compartilhadas!";
    }
  },
  share: {
    icon: "upload",
    detail: function() {
      return "Compartilhando as legendas desse vídeo...";
    }
  },
  error: {
    icon: "error",
    detail: function() {
      return "Erro " + this.error;
    }
  }
};

module.exports = function (React) {
  var possibleStatus = ['init', 'info', 'upload', 'search', 'download', 'downloaded', 'notfound', 'unchanged', 'uploaded', 'share', 'error'];

  return React.createClass({
    displayName: 'Search',

    propTypes: {
      status: React.PropTypes.oneOf(possibleStatus).isRequired
    },

    iconPath: function (icon) {
      return "images/icon-" + icon + ".svg";
    },

    render: function () {
      var status = this.props.status;

      var info = statusMap[status];
      var className = "search flex-row " + status;

      return (
        <div className={className}>
          <img src={this.iconPath(info.icon)} className="status" />
          <div className="info flex">
            <div className="status">basePath</div>
            <div className="detail">{info.detail()}</div>
          </div>
          <div className="actions">
            <div id="close"><img src="images/icon-close.svg"/></div>
            <div id="view"><img src="images/icon-view.svg"/></div>
            <div id="alternatives"><img src="images/icon-plus.svg"/></div>
          </div>
        </div>
      );
    }
  });
};
